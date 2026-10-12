package com.example.background

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.Routine
import com.example.data.model.TriggerType
import com.example.service.RoutineExecutionEngine
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * Decides which Battery, Wi-Fi and "App opened" routines to run or restore when the device state
 * changes. Fed by [TriggerMonitorService]. Transitions are remembered in SharedPreferences so the
 * same state never fires a routine twice (including across service and process restarts).
 */
object TriggerEvaluator {

    private const val TAG = "TriggerEvaluator"
    private const val PREFS = "trigger_state_prefs"

    /** Serializes evaluations so two events can't read and write the same transition state at once. */
    private val mutex = Mutex()

    suspend fun onBatteryState(context: Context, batteryPct: Int, isCharging: Boolean) = evaluate(context, TriggerType.BATTERY) { routine, engine, prefs ->
        val config = JSONObject(routine.triggerConfigJson)
        val reqCharging = config.optBoolean("charging", false)
        val targetLevel = config.optInt("level", 20)

        if (reqCharging) {
            val key = "last_charging_${routine.id}"
            val wasCharging = prefs.getBoolean(key, false)
            if (isCharging && !wasCharging) {
                prefs.edit().putBoolean(key, true).apply()
                engine.executeRoutine(routine, "Charging Connected")
            } else if (!isCharging && wasCharging) {
                prefs.edit().putBoolean(key, false).apply()
                if (routine.restoreOnEnd) engine.restoreRoutineSettings(routine, "Charging Disconnected")
            }
        } else {
            val wasLowKey = "battery_was_low_${routine.id}"
            val wasLow = prefs.getBoolean(wasLowKey, false)
            val isBelowThreshold = batteryPct in 1..targetLevel
            if (isBelowThreshold && !isCharging && !wasLow) {
                prefs.edit().putBoolean(wasLowKey, true).apply()
                engine.executeRoutine(routine, "Battery Low ($batteryPct%)")
            } else if (wasLow && (batteryPct > targetLevel + 5 || (isCharging && batteryPct > targetLevel))) {
                // Re-arm once the battery has recovered, so the next drop triggers again
                prefs.edit().putBoolean(wasLowKey, false).apply()
                if (routine.restoreOnEnd) engine.restoreRoutineSettings(routine, "Battery Recharged to $batteryPct%")
            }
        }
    }

    /**
     * @param ssid the connected network name without quotes, or blank when unknown
     *             (Android hides it without location permission).
     */
    suspend fun onWifiState(context: Context, isConnected: Boolean, ssid: String) = evaluate(context, TriggerType.WIFI) { routine, engine, prefs ->
        val config = JSONObject(routine.triggerConfigJson)
        val reqConnected = config.optBoolean("connected", true)
        val targetSsid = config.optString("ssid", "").trim().trim('"')
        val anyNetwork = targetSsid.isBlank() || targetSsid.equals("Any Network", ignoreCase = true)

        val stateKey = "wifi_last_connected_${routine.id}"
        val ssidKey = "wifi_last_ssid_${routine.id}"
        val wasActive = prefs.getBoolean(stateKey, false)
        val lastSsid = prefs.getString(ssidKey, "") ?: ""

        // A routine for a specific network can only match once the SSID is known.
        if (isConnected && ssid.isBlank() && !anyNetwork) return@evaluate
        val onTarget = isConnected && (anyNetwork || ssid.equals(targetSsid, ignoreCase = true))

        if (reqConnected) {
            // "Active" = connected to the target network
            if (onTarget && !wasActive) {
                prefs.edit().putBoolean(stateKey, true).putString(ssidKey, ssid).apply()
                engine.executeRoutine(routine, "Wi-Fi: Connected to ${ssid.ifBlank { "Wi-Fi" }}")
            } else if (!onTarget && wasActive) {
                prefs.edit().putBoolean(stateKey, false).putString(ssidKey, "").apply()
                if (routine.restoreOnEnd) engine.restoreRoutineSettings(routine, "Wi-Fi: Disconnected from ${lastSsid.ifBlank { "Wi-Fi" }}")
            }
        } else {
            // "Active" = disconnected from the target network (any network: from Wi-Fi entirely)
            val connectedKey = "wifi_was_on_target_${routine.id}"
            val wasOnTarget = prefs.getBoolean(connectedKey, false)
            if (onTarget) {
                if (!wasOnTarget) prefs.edit().putBoolean(connectedKey, true).apply()
                if (wasActive) {
                    prefs.edit().putBoolean(stateKey, false).apply()
                    if (routine.restoreOnEnd) engine.restoreRoutineSettings(routine, "Wi-Fi: Reconnected to ${ssid.ifBlank { "Wi-Fi" }}")
                }
            } else if (wasOnTarget && !wasActive) {
                prefs.edit().putBoolean(stateKey, true).putBoolean(connectedKey, false).apply()
                engine.executeRoutine(routine, "Wi-Fi: Disconnected${if (anyNetwork) "" else " from $targetSsid"}")
            }
        }
    }

    suspend fun onForegroundAppChanged(context: Context, previousPackage: String?, currentPackage: String) =
        evaluate(context, TriggerType.APP) { routine, engine, _ ->
            val config = JSONObject(routine.triggerConfigJson)
            val targetPackage = config.optString("packageName", "")
            if (targetPackage.isBlank()) return@evaluate
            val appName = config.optString("appName", targetPackage)

            if (currentPackage == targetPackage && previousPackage != targetPackage) {
                engine.executeRoutine(routine, "App opened: $appName")
            } else if (previousPackage == targetPackage && currentPackage != targetPackage) {
                if (routine.restoreOnEnd) engine.restoreRoutineSettings(routine, "App closed: $appName")
            }
        }

    private suspend fun evaluate(
        context: Context,
        type: TriggerType,
        block: suspend (Routine, RoutineExecutionEngine, android.content.SharedPreferences) -> Unit
    ) {
        val appContext = context.applicationContext
        mutex.withLock {
            val routines = try {
                AppDatabase.getDatabase(appContext).routineDao().getActiveRoutines().filter { it.triggerType == type }
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading $type routines", e)
                return
            }
            if (routines.isEmpty()) return

            val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val engine = RoutineExecutionEngine(appContext)
            val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PixelRoutines:TriggerEvaluator")
                ?.apply {
                    setReferenceCounted(false)
                    acquire(120_000L)
                }
            try {
                for (routine in routines) {
                    try {
                        block(routine, engine, prefs)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error evaluating $type trigger for routine ${routine.id}", e)
                    }
                }
            } finally {
                try {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                } catch (e: Throwable) {
                    // Ignore
                }
                WidgetUpdateHelper.updateAllWidgets(appContext)
            }
        }
    }
}
