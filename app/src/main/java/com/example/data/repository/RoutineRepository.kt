package com.example.data.repository

import android.content.Context
import com.example.data.local.RoutineDao
import com.example.data.model.ActionType
import com.example.data.model.RoutineActionItem
import com.example.data.model.RoutineEntity
import com.example.data.model.RoutineExecutionLog
import com.example.data.model.TriggerType
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val context: Context? = null
) {

    companion object {
        private const val PREFS_NAME = "pixel_routines_prefs"
        private const val PREF_DEFAULTS_POPULATED = "routines_defaults_populated_v1"
    }

    val allRoutines: Flow<List<RoutineEntity>> = routineDao.getAllRoutines()
    val allLogs: Flow<List<RoutineExecutionLog>> = routineDao.getAllLogs()

    suspend fun getActiveRoutines(): List<RoutineEntity> = routineDao.getActiveRoutines()

    suspend fun getRoutineById(id: Long): RoutineEntity? = routineDao.getRoutineById(id)

    suspend fun insertRoutine(routine: RoutineEntity): Long = routineDao.insertRoutine(routine)

    suspend fun updateRoutine(routine: RoutineEntity) = routineDao.updateRoutine(routine)

    suspend fun deleteRoutine(routine: RoutineEntity) {
        routineDao.deleteRoutineById(routine.id)
        routineDao.deleteLogsForRoutine(routine.id)
    }

    suspend fun deleteRoutineById(id: Long) {
        routineDao.deleteRoutineById(id)
        routineDao.deleteLogsForRoutine(id)
    }

    suspend fun toggleRoutine(id: Long, isEnabled: Boolean) = routineDao.updateRoutineEnabled(id, isEnabled)

    suspend fun updateExecutionStatus(id: Long, timestamp: Long, status: String) =
        routineDao.updateExecutionStatus(id, timestamp, status)

    suspend fun recordExecutionLog(log: RoutineExecutionLog) = routineDao.insertLog(log)

    fun getLogsForRoutine(routineId: Long): Flow<List<RoutineExecutionLog>> = routineDao.getLogsForRoutine(routineId)

    suspend fun deleteLogsForRoutine(routineId: Long) = routineDao.deleteLogsForRoutine(routineId)

    suspend fun clearLogs() = routineDao.clearLogs()

    /**
     * Seeds initial sample routines strictly ONCE upon fresh installation.
     * Prevents deleted routines from resurrecting and prevents turned-off routines
     * from being overwritten with enabled defaults.
     */
    suspend fun populateDefaultsIfEmpty(context: Context? = null) {
        val targetContext = context ?: this.context
        val prefs = targetContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. If defaults were already initialized on this device, never re-populate
        if (prefs != null && prefs.getBoolean(PREF_DEFAULTS_POPULATED, false)) {
            return
        }

        // 2. Even if flag is unset, check TOTAL routine count (both enabled and disabled).
        // If the user already has routines in the database, do NOT insert defaults.
        val totalCount = routineDao.getTotalRoutineCount()
        if (totalCount > 0) {
            prefs?.edit()?.putBoolean(PREF_DEFAULTS_POPULATED, true)?.apply()
            return
        }

        // 3. Database is completely empty on initial installation: insert defaults once
        // 1. Bedtime Automation
        val bedtimeTrigger = JSONObject().apply {
            put("startTime", "22:30")
            put("endTime", "07:00")
            put("days", "Mon, Tue, Wed, Thu, Fri, Sat, Sun")
        }.toString()

        val bedtimeActions = JSONArray().apply {
            put(RoutineActionItem(type = ActionType.AOD, enabledState = false).toJsonObject())
            put(RoutineActionItem(type = ActionType.SOUND_PROFILE, stringValue = "SILENT", intValue = 0).toJsonObject())
            put(RoutineActionItem(type = ActionType.BATTERY_SAVER, enabledState = true).toJsonObject())
            put(RoutineActionItem(type = ActionType.SCREEN_TIMEOUT, intValue = 30000).toJsonObject())
        }.toString()

        routineDao.insertRoutine(
            RoutineEntity(
                title = "Bedtime Silence & AOD Off",
                description = "Turns off Always-On Display, silences ringer, and engages battery saver overnight",
                isEnabled = true,
                iconName = "bedtime",
                colorHex = "#5E5691",
                triggerType = TriggerType.TIME,
                triggerConfigJson = bedtimeTrigger,
                actionsJson = bedtimeActions
            )
        )

        // 2. Battery Safeguard
        val batteryTrigger = JSONObject().apply {
            put("level", 20)
            put("charging", false)
        }.toString()

        val batteryActions = JSONArray().apply {
            put(RoutineActionItem(type = ActionType.BATTERY_SAVER, enabledState = true).toJsonObject())
            put(RoutineActionItem(type = ActionType.AOD, enabledState = false).toJsonObject())
            put(RoutineActionItem(type = ActionType.SCREEN_TIMEOUT, intValue = 15000).toJsonObject())
            put(RoutineActionItem(type = ActionType.DARK_MODE, enabledState = true).toJsonObject())
        }.toString()

        routineDao.insertRoutine(
            RoutineEntity(
                title = "Extreme Battery Preserver",
                description = "Triggers when battery drops below 20% to prevent unexpected shutdown",
                isEnabled = true,
                iconName = "battery_alert",
                colorHex = "#B3261E",
                triggerType = TriggerType.BATTERY,
                triggerConfigJson = batteryTrigger,
                actionsJson = batteryActions
            )
        )

        // 3. Work Wi-Fi Arrival
        val wifiTrigger = JSONObject().apply {
            put("ssid", "Office_Secure_5G")
            put("connected", true)
        }.toString()

        val wifiActions = JSONArray().apply {
            put(RoutineActionItem(type = ActionType.SOUND_PROFILE, stringValue = "VIBRATE", intValue = 20).toJsonObject())
            put(RoutineActionItem(type = ActionType.AUTO_ROTATE, enabledState = false).toJsonObject())
        }.toString()

        routineDao.insertRoutine(
            RoutineEntity(
                title = "Work Environment",
                description = "Sets vibrate mode and locks screen orientation upon joining office network",
                isEnabled = false,
                iconName = "business",
                colorHex = "#0B57D0",
                triggerType = TriggerType.WIFI,
                triggerConfigJson = wifiTrigger,
                actionsJson = wifiActions
            )
        )

        // 4. Sunset Dark Theme
        val sunsetTrigger = JSONObject().apply {
            put("solarEvent", "SUNSET")
            put("offsetMinutes", 0)
            put("days", "Daily")
            put("useExactAlarm", true)
        }.toString()

        val sunsetActions = JSONArray().apply {
            put(RoutineActionItem(type = ActionType.DARK_MODE, enabledState = true).toJsonObject())
            put(RoutineActionItem(type = ActionType.AOD, enabledState = false).toJsonObject())
        }.toString()

        routineDao.insertRoutine(
            RoutineEntity(
                title = "Sunset Dark Theme",
                description = "Switches to Dark Theme automatically at sunset based on local astronomical calculations",
                isEnabled = true,
                iconName = "nightlight",
                colorHex = "#5E35B1",
                triggerType = TriggerType.SUNSET,
                triggerConfigJson = sunsetTrigger,
                actionsJson = sunsetActions
            )
        )

        // Mark permanently as populated so user deletions or state toggles are never overwritten
        prefs?.edit()?.putBoolean(PREF_DEFAULTS_POPULATED, true)?.apply()
    }
}
