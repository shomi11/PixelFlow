package com.vpmapps.pixelflow.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Supported Trigger Types for "If This" condition.
 */
enum class TriggerType(val displayName: String, val description: String) {
    TIME("Time Schedule", "Active during specific hours and days"),
    SUNRISE("Sunrise", "At sunrise based on your real location"),
    SUNSET("Sunset", "At sunset based on your real location"),
    LOCATION("Geolocation", "When arriving at or leaving a specific location"),
    WIFI("Wi-Fi Network", "When connected or disconnected from a Wi-Fi SSID"),
    BLUETOOTH("Bluetooth Device", "When connected or disconnected from a Bluetooth device"),
    BATTERY("Battery Level", "When battery level drops below a threshold or charging state changes"),
    APP("App Opened", "When a specific application is launched into the foreground");

    /** Time, sunrise and sunset share one alarm slot, so a routine can have only one of them. */
    val isTimeBased: Boolean
        get() = this == TIME || this == SUNRISE || this == SUNSET
}

/**
 * Supported Action Types for "Then That" execution.
 */
enum class ActionType(val displayName: String, val category: String, val requiresShizuku: Boolean) {
    AOD("Always-On Display (AOD)", "Display", true),
    AUTO_ROTATE("Auto-Rotate", "Display", true),
    DARK_MODE("Dark Theme", "Display", true),
    SOUND_PROFILE("Sound Profile", "Audio", false),
    WIFI("Wi-Fi Power", "Connectivity", true),
    HOTSPOT("Wi-Fi Hotspot", "Connectivity", true),
    BLUETOOTH("Bluetooth Power", "Connectivity", true),
    BATTERY_SAVER("Battery Saver", "Power", true),
    SCREEN_TIMEOUT("Screen Timeout", "Display", true),
    CUSTOM_NOTIFICATION("Custom Notification", "Notification", false)
}

/**
 * Configuration payload for Custom Notification actions.
 */
data class CustomNotificationConfig(
    val title: String = "Reminder",
    val message: String = "",
    val iconName: String = "bell",
    val scheduleType: String = "IMMEDIATE", // "IMMEDIATE", "ONCE", "DAILY"
    val time: String = "09:00"
) {
    fun toJsonString(): String {
        return JSONObject().apply {
            put("title", title)
            put("message", message)
            put("iconName", iconName)
            put("scheduleType", scheduleType)
            put("time", time)
        }.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): CustomNotificationConfig {
            return try {
                if (jsonStr.isBlank() || !jsonStr.trim().startsWith("{")) {
                    CustomNotificationConfig(title = jsonStr.ifBlank { "Reminder" })
                } else {
                    val json = JSONObject(jsonStr)
                    CustomNotificationConfig(
                        title = json.optString("title", "Reminder"),
                        message = json.optString("message", ""),
                        iconName = json.optString("iconName", "bell"),
                        scheduleType = json.optString("scheduleType", "IMMEDIATE"),
                        time = json.optString("time", "09:00")
                    )
                }
            } catch (e: Exception) {
                CustomNotificationConfig(title = jsonStr.ifBlank { "Reminder" })
            }
        }
    }
}

/**
 * Concrete trigger representation associated with a Routine.
 */
data class RoutineTrigger(
    val type: TriggerType = TriggerType.TIME,
    val configJson: String = "{}",
    val displayName: String = type.displayName
) {
    /**
     * True for conditions that happen at a moment (a time of day, sunrise, sunset) rather than
     * describing a state that stays true for a while. A TIME condition with an end time is a window,
     * which is a state.
     */
    val isMoment: Boolean
        get() = when (type) {
            TriggerType.SUNRISE, TriggerType.SUNSET -> true
            TriggerType.TIME -> !isTimeWindow
            else -> false
        }

    /** TIME condition with a distinct end time, e.g. 22:00 - 07:00. */
    val isTimeWindow: Boolean
        get() {
            if (type != TriggerType.TIME) return false
            return try {
                val json = JSONObject(configJson)
                val start = json.optString("startTime", "")
                val end = json.optString("endTime", "")
                json.optString("scheduleMode", "DAILY") != "INTERVAL" && end.isNotBlank() && end != start
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Whether [now] falls inside this TIME window. The window includes its start minute and excludes
     * its end minute, so the end alarm reliably sees the window as over. For overnight windows the
     * part after midnight belongs to the day the window started.
     */
    fun isWithinTimeWindow(now: java.util.Calendar): Boolean {
        if (!isTimeWindow) return false
        return try {
            val json = JSONObject(configJson)
            val parsedStart = com.vpmapps.pixelflow.util.TimeParser.parseToHourMinute(json.optString("startTime", "")) ?: return false
            val parsedEnd = com.vpmapps.pixelflow.util.TimeParser.parseToHourMinute(json.optString("endTime", "")) ?: return false
            val startMin = parsedStart.first * 60 + parsedStart.second
            val endMin = parsedEnd.first * 60 + parsedEnd.second
            val currentMin = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
            val days = json.optString("days", "Daily")

            if (startMin < endMin) {
                currentMin in startMin until endMin && isScheduledDay(days, now)
            } else if (currentMin >= startMin) {
                isScheduledDay(days, now)
            } else if (currentMin < endMin) {
                val windowStartDay = (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
                isScheduledDay(days, windowStartDay)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isScheduledDay(days: String, day: java.util.Calendar): Boolean {
        if (days.isBlank() || days.equals("Daily", ignoreCase = true) || days.equals("Every Day", ignoreCase = true)) return true
        val dayName = when (day.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "Mon"
            java.util.Calendar.TUESDAY -> "Tue"
            java.util.Calendar.WEDNESDAY -> "Wed"
            java.util.Calendar.THURSDAY -> "Thu"
            java.util.Calendar.FRIDAY -> "Fri"
            java.util.Calendar.SATURDAY -> "Sat"
            else -> "Sun"
        }
        return days.contains(dayName, ignoreCase = true)
    }

    fun summary(): String {
        return try {
            val json = JSONObject(configJson)
            when (type) {
                TriggerType.TIME -> {
                    val mode = json.optString("scheduleMode", "DAILY")
                    val start = json.optString("startTime", "08:00")
                    val days = json.optString("days", "Daily")
                    val interval = json.optInt("intervalMinutes", 0)
                    val parsed = com.vpmapps.pixelflow.util.TimeParser.parseToHourMinute(start)
                    val friendlyTime = if (parsed != null) {
                        com.vpmapps.pixelflow.util.TimeParser.format12Hour(parsed.first, parsed.second)
                    } else start

                    val end = com.vpmapps.pixelflow.util.TimeParser.parseToHourMinute(json.optString("endTime", ""))
                    if (isTimeWindow && end != null) {
                        val friendlyEnd = com.vpmapps.pixelflow.util.TimeParser.format12Hour(end.first, end.second)
                        val dayText = if (days.isBlank() || days.equals("Daily", ignoreCase = true) ||
                            days == "Mon, Tue, Wed, Thu, Fri, Sat, Sun") "" else " ($days)"
                        return "$friendlyTime – $friendlyEnd$dayText"
                    }
                    when (mode) {
                        "INTERVAL" -> if (interval >= 60) "Every ${interval / 60}h" else "Every ${interval}m"
                        "WEEKLY" -> "Weekly ($days) at $friendlyTime"
                        "SPECIFIC_TIME" -> "At $friendlyTime"
                        else -> "Daily at $friendlyTime"
                    }
                }
                TriggerType.SUNRISE -> {
                    val offset = json.optInt("offsetMinutes", 0)
                    val days = json.optString("days", "Daily")
                    val offsetText = when {
                        offset < 0 -> "${-offset}m before Sunrise"
                        offset > 0 -> "${offset}m after Sunrise"
                        else -> "At Sunrise"
                    }
                    if (days.isBlank() || days.equals("Daily", ignoreCase = true) || days.equals("Every Day", ignoreCase = true)) {
                        offsetText
                    } else {
                        "$offsetText ($days)"
                    }
                }
                TriggerType.SUNSET -> {
                    val offset = json.optInt("offsetMinutes", 0)
                    val days = json.optString("days", "Daily")
                    val offsetText = when {
                        offset < 0 -> "${-offset}m before Sunset"
                        offset > 0 -> "${offset}m after Sunset"
                        else -> "At Sunset"
                    }
                    if (days.isBlank() || days.equals("Daily", ignoreCase = true) || days.equals("Every Day", ignoreCase = true)) {
                        offsetText
                    } else {
                        "$offsetText ($days)"
                    }
                }
                TriggerType.LOCATION -> {
                    val label = json.optString("label", "Geofence")
                    val transition = json.optString("transition", "ENTER")
                    val radius = json.optInt("radiusMeters", 150)
                    val event = if (transition == "EXIT") "Leaving" else "Arriving at"
                    "$event \"$label\" (${radius}m)"
                }
                TriggerType.WIFI -> {
                    val ssid = json.optString("ssid", "Any Network")
                    val state = if (json.optBoolean("connected", true)) "Connected to" else "Disconnected from"
                    "$state \"$ssid\""
                }
                TriggerType.BLUETOOTH -> {
                    val device = json.optString("device", "Any Device")
                    val state = if (json.optBoolean("connected", true)) "Connected to" else "Disconnected from"
                    "$state \"$device\""
                }
                TriggerType.BATTERY -> {
                    val level = json.optInt("level", 20)
                    val charging = json.optBoolean("charging", false)
                    if (charging) "When Charging" else "Battery drops below $level%"
                }
                TriggerType.APP -> {
                    val app = json.optString("appName", json.optString("packageName", "Selected App"))
                    "When $app opens"
                }
            }
        } catch (e: Exception) {
            type.displayName
        }
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("type", type.name)
            put("configJson", configJson)
            put("displayName", displayName)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): RoutineTrigger {
            val typeStr = json.optString("type", TriggerType.TIME.name)
            val type = try {
                TriggerType.valueOf(typeStr)
            } catch (e: Exception) {
                TriggerType.TIME
            }
            return RoutineTrigger(
                type = type,
                configJson = json.optString("configJson", "{}"),
                displayName = json.optString("displayName", type.displayName)
            )
        }
    }
}

/**
 * Concrete action representation associated with a Routine.
 */
data class RoutineActionItem(
    val type: ActionType = ActionType.AOD,
    val enabledState: Boolean = true,
    val intValue: Int = 0, // e.g. timeout in ms or volume level
    val stringValue: String = "" // e.g. "VIBRATE", "SILENT", "NORMAL"
) {
    fun toSummary(): String {
        return when (type) {
            ActionType.AOD -> if (enabledState) "AOD: On" else "AOD: Off"
            ActionType.AUTO_ROTATE -> if (enabledState) "Auto-Rotate: On" else "Auto-Rotate: Off"
            ActionType.DARK_MODE -> if (enabledState) "Dark Mode: On" else "Dark Mode: Off"
            ActionType.SOUND_PROFILE -> {
                when (stringValue.uppercase()) {
                    "SILENT" -> "Sound: Silent (DND)"
                    "VIBRATE" -> "Sound: Vibrate Mode"
                    "NORMAL", "RING" -> {
                        if (intValue in 0..100) "Sound: Ring (${intValue}% Vol)"
                        else "Sound: Ring Mode"
                    }
                    else -> "Sound: $stringValue"
                }
            }
            ActionType.WIFI -> if (enabledState) "Wi-Fi: On" else "Wi-Fi: Off"
            ActionType.HOTSPOT -> if (enabledState) "Hotspot: On" else "Hotspot: Off"
            ActionType.BLUETOOTH -> if (enabledState) "Bluetooth: On" else "Bluetooth: Off"
            ActionType.BATTERY_SAVER -> if (enabledState) "Battery Saver: On" else "Battery Saver: Off"
            ActionType.SCREEN_TIMEOUT -> "Timeout: ${intValue / 1000}s"
            ActionType.CUSTOM_NOTIFICATION -> {
                val cfg = CustomNotificationConfig.fromJsonString(stringValue)
                val timingStr = when (cfg.scheduleType) {
                    "DAILY" -> "Daily at ${cfg.time}"
                    "ONCE" -> "Once at ${cfg.time}"
                    else -> "When active"
                }
                "Notify: \"${cfg.title}\" ($timingStr)"
            }
        }
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("type", type.name)
            put("enabledState", enabledState)
            put("intValue", intValue)
            put("stringValue", stringValue)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): RoutineActionItem {
            val typeStr = json.optString("type", ActionType.AOD.name)
            val type = try {
                ActionType.valueOf(typeStr)
            } catch (e: Exception) {
                ActionType.AOD
            }
            return RoutineActionItem(
                type = type,
                enabledState = json.optBoolean("enabledState", true),
                intValue = json.optInt("intValue", 0),
                stringValue = json.optString("stringValue", "")
            )
        }
    }
}

/**
 * Routine data class and Room Entity representing an automation rule,
 * containing fields for routine name, enabled status, and lists of
 * associated triggers and actions.
 */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "isEnabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "triggers")
    val triggers: List<RoutineTrigger> = emptyList(),

    @ColumnInfo(name = "actions")
    val actions: List<RoutineActionItem> = emptyList(),

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "iconName")
    val iconName: String = "schedule",

    @ColumnInfo(name = "colorHex")
    val colorHex: String = "#3871E0",

    @ColumnInfo(name = "lastExecutedTimestamp")
    val lastExecutedTimestamp: Long = 0L,

    @ColumnInfo(name = "lastExecutionStatus")
    val lastExecutionStatus: String = "IDLE", // IDLE, SUCCESS, FAILED, SIMULATED

    @ColumnInfo(name = "restoreOnEnd", defaultValue = "1")
    val restoreOnEnd: Boolean = true,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    /** [MATCH_ALL]: every condition must be met. [MATCH_ANY]: one met condition is enough. */
    @ColumnInfo(name = "matchMode", defaultValue = "ALL")
    val matchMode: String = MATCH_ALL
) {
    // Backward compatibility getters
    val title: String get() = name

    val triggerType: TriggerType
        get() = triggers.firstOrNull()?.type ?: TriggerType.TIME

    val triggerConfigJson: String
        get() = triggers.firstOrNull()?.configJson ?: "{}"

    val actionsJson: String
        get() {
            val arr = JSONArray()
            actions.forEach { arr.put(it.toJsonObject()) }
            return arr.toString()
        }

    fun parseActions(): List<RoutineActionItem> = actions

    fun getTriggerSummary(): String {
        if (triggers.isEmpty()) return "Manual"
        val joiner = if (matchMode == MATCH_ANY) " or " else " and "
        return triggers.joinToString(joiner) { it.summary() }
    }

    /** The routine's single time-based condition (time, sunrise or sunset), if it has one. */
    val timeCondition: RoutineTrigger?
        get() = triggers.firstOrNull { it.type.isTimeBased }

    fun condition(type: TriggerType): RoutineTrigger? = triggers.firstOrNull { it.type == type }

    fun hasCondition(type: TriggerType): Boolean = triggers.any { it.type == type }

    /**
     * True while the routine's time window (if it has one) covers the current time.
     */
    fun isCurrentlyInActiveWindow(): Boolean {
        if (!isEnabled) return false
        return timeCondition?.isWithinTimeWindow(java.util.Calendar.getInstance()) == true
    }

    @Ignore
    constructor(
        id: Long = 0,
        title: String,
        description: String = "",
        isEnabled: Boolean = true,
        iconName: String = "schedule",
        colorHex: String = "#3871E0",
        triggerType: TriggerType,
        triggerConfigJson: String,
        actionsJson: String,
        lastExecutedTimestamp: Long = 0L,
        lastExecutionStatus: String = "IDLE",
        restoreOnEnd: Boolean = true,
        createdAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        name = title,
        isEnabled = isEnabled,
        triggers = listOf(RoutineTrigger(type = triggerType, configJson = triggerConfigJson)),
        actions = parseActionsJson(actionsJson),
        description = description,
        iconName = iconName,
        colorHex = colorHex,
        lastExecutedTimestamp = lastExecutedTimestamp,
        lastExecutionStatus = lastExecutionStatus,
        restoreOnEnd = restoreOnEnd,
        createdAt = createdAt
    )

    companion object {
        const val MATCH_ALL = "ALL"
        const val MATCH_ANY = "ANY"

        fun parseActionsJson(jsonStr: String): List<RoutineActionItem> {
            val list = mutableListOf<RoutineActionItem>()
            if (jsonStr.isBlank()) return list
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    list.add(RoutineActionItem.fromJsonObject(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                // Ignore parse error
            }
            return list
        }
    }
}

/**
 * Type alias to preserve compatibility across existing references.
 */
typealias RoutineEntity = Routine
