package com.example.data.model

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
    WIFI("Wi-Fi Network", "When connected or disconnected from a Wi-Fi SSID"),
    BLUETOOTH("Bluetooth Device", "When connected or disconnected from a Bluetooth device"),
    BATTERY("Battery Level", "When battery level drops below a threshold or charging state changes"),
    APP("App Opened", "When a specific application is launched into the foreground")
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
    BLUETOOTH("Bluetooth Power", "Connectivity", true),
    BATTERY_SAVER("Battery Saver", "Power", true),
    SCREEN_TIMEOUT("Screen Timeout", "Display", true)
}

/**
 * Concrete trigger representation associated with a Routine.
 */
data class RoutineTrigger(
    val type: TriggerType = TriggerType.TIME,
    val configJson: String = "{}",
    val displayName: String = type.displayName
) {
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
            ActionType.BLUETOOTH -> if (enabledState) "Bluetooth: On" else "Bluetooth: Off"
            ActionType.BATTERY_SAVER -> if (enabledState) "Battery Saver: On" else "Battery Saver: Off"
            ActionType.SCREEN_TIMEOUT -> "Timeout: ${intValue / 1000}s"
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

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
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
        val trigger = triggers.firstOrNull() ?: return "Manual"
        return try {
            val json = JSONObject(trigger.configJson)
            when (trigger.type) {
                TriggerType.TIME -> {
                    val mode = json.optString("scheduleMode", "DAILY")
                    val start = json.optString("startTime", "08:00")
                    val days = json.optString("days", "Daily")
                    val interval = json.optInt("intervalMinutes", 0)
                    val parsed = com.example.util.TimeParser.parseToHourMinute(start)
                    val friendlyTime = if (parsed != null) {
                        com.example.util.TimeParser.format12Hour(parsed.first, parsed.second)
                    } else start

                    when (mode) {
                        "INTERVAL" -> if (interval >= 60) "Every ${interval / 60}h" else "Every ${interval}m"
                        "WEEKLY" -> "Weekly ($days) at $friendlyTime"
                        "SPECIFIC_TIME" -> "At $friendlyTime"
                        else -> "Daily at $friendlyTime"
                    }
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
            trigger.type.displayName
        }
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
        createdAt = createdAt
    )

    companion object {
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
