package com.vpmapps.pixelflow.background

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.vpmapps.pixelflow.data.local.AppDatabase
import com.vpmapps.pixelflow.data.model.Routine
import com.vpmapps.pixelflow.data.model.RoutineTrigger
import com.vpmapps.pixelflow.data.model.TriggerType
import com.vpmapps.pixelflow.service.RoutineExecutionEngine
import com.vpmapps.pixelflow.util.LocationHelper
import com.vpmapps.pixelflow.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/** What prompted a re-check of routine conditions. */
sealed interface ConditionEvent {
    /** Some device state changed (Wi-Fi, battery, Bluetooth, location, app, a time window edge). */
    data object StateChanged : ConditionEvent

    /** The time, sunrise or sunset moment of this routine has arrived. */
    data class MomentReached(val routineId: Long) : ConditionEvent
}

/** What a routine should do after a re-check. */
enum class RoutineTransition {
    /** Conditions are met: run the actions and mark the routine as on. */
    START,

    /** Conditions are no longer met: mark the routine as off and restore settings if enabled. */
    END,

    /** A moment-only routine fired: run the actions, there is no "on" period to end. */
    RUN_ONCE,
    NONE
}

/**
 * Decides when routines start and end, Samsung "Modes and Routines" style.
 *
 * A routine has one or more conditions, matched with All (default) or Any:
 * - State conditions stay true for a while: a time window, Wi-Fi network, Bluetooth device,
 *   battery level or charging, being at a location, an app being open. The routine is on while
 *   they are met and ends when they stop being met.
 * - Moment conditions happen at a point in time: a time of day, sunrise, sunset. The routine runs
 *   at that moment if its other conditions are met at that moment.
 *
 * Every event source (alarms, Wi-Fi, battery, Bluetooth, geofences, app changes) updates
 * [DeviceFactsStore] and calls [evaluate]; this object is the only place that runs or restores
 * routines because of conditions.
 */
object TriggerEvaluator {

    private const val TAG = "TriggerEvaluator"

    /** Extra battery percent a running low-battery routine tolerates, so it doesn't flap at the threshold. */
    private const val BATTERY_HYSTERESIS = 3

    private val mutex = Mutex()

    // --- Event entry points ---

    suspend fun onBatteryState(context: Context, batteryPct: Int, isCharging: Boolean) =
        evaluate(context) { it.copy(batteryPct = batteryPct, isCharging = isCharging) }

    /** @param ssid network name without quotes, or blank when Android hides it. */
    suspend fun onWifiState(context: Context, isConnected: Boolean, ssid: String) {
        DeviceFactsStore.setWifi(context, isConnected, ssid)
        evaluate(context) { it.copy(wifiConnected = isConnected, wifiSsid = if (isConnected) ssid else "") }
    }

    suspend fun onForegroundAppChanged(context: Context, previousPackage: String?, currentPackage: String) {
        DeviceFactsStore.setForegroundApp(context, currentPackage)
        evaluate(context) { it.copy(foregroundApp = currentPackage) }
    }

    suspend fun onBluetoothChanged(context: Context, name: String, address: String, connected: Boolean) {
        DeviceFactsStore.setBluetoothConnected(context, name, address, connected)
        evaluate(context)
    }

    suspend fun onGeofenceChanged(context: Context, routineId: Long, inside: Boolean) {
        DeviceFactsStore.setGeofenceInside(context, routineId, inside)
        evaluate(context, routineId = routineId)
    }

    /** Fire-and-forget re-check, e.g. after a routine was saved or enabled. */
    fun evaluateAsync(context: Context, routineId: Long? = null) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch { evaluate(appContext, routineId = routineId) }
    }

    /**
     * Re-checks enabled routines (or only [routineId]) and starts or ends them as needed.
     *
     * @param adjustFacts lets an event source supply fresher values than the stored snapshot
     */
    suspend fun evaluate(
        context: Context,
        event: ConditionEvent = ConditionEvent.StateChanged,
        routineId: Long? = null,
        adjustFacts: (DeviceFacts) -> DeviceFacts = { it }
    ) {
        val appContext = context.applicationContext
        mutex.withLock {
            val routines = try {
                val dao = AppDatabase.getDatabase(appContext).routineDao()
                if (routineId != null) {
                    listOfNotNull(dao.getRoutineById(routineId)?.takeIf { it.isEnabled })
                } else {
                    dao.getActiveRoutines()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading routines", e)
                return
            }.filter { it.triggers.isNotEmpty() }
            if (routines.isEmpty()) return

            var facts = adjustFacts(DeviceFactsStore.snapshot(appContext))
            facts = fillUnknownGeofences(appContext, routines, facts)

            val engine = RoutineExecutionEngine(appContext)
            val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PixelRoutines:TriggerEvaluator")
                ?.apply {
                    setReferenceCounted(false)
                    acquire(120_000L)
                }
            var changed = false
            try {
                for (routine in routines) {
                    try {
                        changed = apply(appContext, engine, routine, facts, event) || changed
                    } catch (e: Exception) {
                        Log.e(TAG, "Error evaluating routine ${routine.id}", e)
                    }
                }
            } finally {
                try {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                } catch (e: Throwable) {
                    // Ignore
                }
                if (changed) WidgetUpdateHelper.updateAllWidgets(appContext)
            }
        }
    }

    private suspend fun apply(
        context: Context,
        engine: RoutineExecutionEngine,
        routine: Routine,
        facts: DeviceFacts,
        event: ConditionEvent
    ): Boolean {
        val wasActive = RoutineActivationStore.isActive(context, routine.id)
        val momentFired = event is ConditionEvent.MomentReached && event.routineId == routine.id
        return when (decide(routine, facts, wasActive, momentFired)) {
            RoutineTransition.START -> {
                RoutineActivationStore.setActive(context, routine.id, true)
                engine.executeRoutine(routine, "Conditions met: ${routine.getTriggerSummary()}")
                true
            }
            RoutineTransition.RUN_ONCE -> {
                engine.executeRoutine(routine, routine.getTriggerSummary())
                true
            }
            RoutineTransition.END -> {
                RoutineActivationStore.setActive(context, routine.id, false)
                if (routine.restoreOnEnd) {
                    engine.restoreRoutineSettings(routine, "Conditions no longer met: ${routine.getTriggerSummary()}")
                }
                true
            }
            RoutineTransition.NONE -> false
        }
    }

    /**
     * Pure decision for one routine, kept separate so it can be unit tested.
     *
     * With All, a routine that also has a moment condition starts only at that moment, and runs
     * again at the next moment while it is still on (e.g. "9 pm on Home Wi-Fi" applies every
     * evening at 9 pm, and ends when you leave the network).
     */
    fun decide(routine: Routine, facts: DeviceFacts, wasActive: Boolean, momentFired: Boolean): RoutineTransition {
        val states = routine.triggers.filterNot { it.isMoment }
        val hasMoment = routine.triggers.any { it.isMoment }
        val stateResults = states.map { isMet(it, routine.id, facts, wasActive) }

        if (routine.matchMode == Routine.MATCH_ANY) {
            val anyState = stateResults.any { it }
            return when {
                anyState && (!wasActive || momentFired) -> RoutineTransition.START
                !anyState && wasActive -> RoutineTransition.END
                momentFired -> RoutineTransition.RUN_ONCE
                else -> RoutineTransition.NONE
            }
        }

        val allStates = stateResults.all { it }
        return when {
            states.isEmpty() -> if (momentFired) RoutineTransition.RUN_ONCE else RoutineTransition.NONE
            wasActive && !allStates -> RoutineTransition.END
            !hasMoment && allStates && !wasActive -> RoutineTransition.START
            hasMoment && allStates && momentFired -> RoutineTransition.START
            else -> RoutineTransition.NONE
        }
    }

    /** Whether one state condition is currently met. */
    fun isMet(trigger: RoutineTrigger, routineId: Long, facts: DeviceFacts, wasActive: Boolean): Boolean {
        val config = try {
            JSONObject(trigger.configJson)
        } catch (e: Exception) {
            return false
        }
        return when (trigger.type) {
            TriggerType.TIME -> trigger.isWithinTimeWindow(facts.now)
            TriggerType.SUNRISE, TriggerType.SUNSET -> false // moments, never a lasting state
            TriggerType.WIFI -> {
                val wantConnected = config.optBoolean("connected", true)
                val target = config.optString("ssid", "").trim().trim('"')
                val anyNetwork = target.isBlank() || target.equals("Any Network", ignoreCase = true)
                val onTarget = when {
                    !facts.wifiConnected -> false
                    anyNetwork -> true
                    // Connected but Android hid the name: keep the current state rather than flapping
                    facts.wifiSsid.isBlank() -> if (wantConnected) wasActive else !wasActive
                    else -> facts.wifiSsid.equals(target, ignoreCase = true)
                }
                if (wantConnected) onTarget else !onTarget
            }
            TriggerType.BLUETOOTH -> {
                val wantConnected = config.optBoolean("connected", true)
                val target = config.optString("device", "").trim()
                val anyDevice = target.isBlank() || target.equals("Any Device", ignoreCase = true) ||
                    target.equals("Any Paired Device", ignoreCase = true)
                val connected = if (anyDevice) {
                    facts.bluetoothDevices.isNotEmpty()
                } else {
                    facts.bluetoothDevices.any { entry ->
                        val name = entry.substringBefore('|')
                        val address = entry.substringAfter('|')
                        name.contains(target, ignoreCase = true) || address.equals(target, ignoreCase = true)
                    }
                }
                if (wantConnected) connected else !connected
            }
            TriggerType.BATTERY -> {
                if (config.optBoolean("charging", false)) {
                    facts.isCharging
                } else {
                    val level = config.optInt("level", 20)
                    val threshold = if (wasActive) level + BATTERY_HYSTERESIS else level
                    !facts.isCharging && facts.batteryPct in 1..threshold
                }
            }
            TriggerType.LOCATION -> {
                val inside = facts.geofenceInside[routineId] ?: return false
                if (config.optString("transition", "ENTER").equals("EXIT", ignoreCase = true)) !inside else inside
            }
            TriggerType.APP -> {
                val target = config.optString("packageName", "")
                target.isNotBlank() && facts.foregroundApp == target
            }
        }
    }

    /**
     * Android only reports geofence crossings, so a location condition has no state until the first
     * crossing. Estimate it from the last known location once, then let crossings take over.
     */
    private fun fillUnknownGeofences(context: Context, routines: List<Routine>, facts: DeviceFacts): DeviceFacts {
        val missing = routines.filter { it.hasCondition(TriggerType.LOCATION) && it.id !in facts.geofenceInside }
        if (missing.isEmpty()) return facts
        val location = try {
            LocationHelper.getLastKnownLocation(context)
        } catch (e: Exception) {
            return facts
        }
        val updated = facts.geofenceInside.toMutableMap()
        for (routine in missing) {
            val config = try {
                JSONObject(routine.condition(TriggerType.LOCATION)!!.configJson)
            } catch (e: Exception) {
                continue
            }
            val lat = config.optDouble("latitude", 0.0)
            val lng = config.optDouble("longitude", 0.0)
            if (lat == 0.0 && lng == 0.0) continue
            val radius = config.optDouble("radiusMeters", 150.0).toFloat()
            val inside = GeofenceManager.computeDistanceMeters(location.latitude, location.longitude, lat, lng) <= radius
            DeviceFactsStore.setGeofenceInside(context, routine.id, inside)
            updated[routine.id] = inside
        }
        return facts.copy(geofenceInside = updated)
    }
}
