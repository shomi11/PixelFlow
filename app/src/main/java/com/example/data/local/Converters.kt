package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.RoutineActionItem
import com.example.data.model.RoutineTrigger
import com.example.data.model.TriggerType
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromTriggerType(type: TriggerType?): String {
        return type?.name ?: TriggerType.TIME.name
    }

    @TypeConverter
    fun toTriggerType(value: String?): TriggerType {
        return try {
            if (value != null) TriggerType.valueOf(value) else TriggerType.TIME
        } catch (e: Exception) {
            TriggerType.TIME
        }
    }

    @TypeConverter
    fun fromTriggerList(triggers: List<RoutineTrigger>?): String {
        if (triggers == null) return "[]"
        val array = JSONArray()
        for (trigger in triggers) {
            array.put(trigger.toJsonObject())
        }
        return array.toString()
    }

    @TypeConverter
    fun toTriggerList(value: String?): List<RoutineTrigger> {
        if (value.isNullOrBlank()) return emptyList()
        val list = mutableListOf<RoutineTrigger>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                list.add(RoutineTrigger.fromJsonObject(array.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // Fallback empty
        }
        return list
    }

    @TypeConverter
    fun fromActionList(actions: List<RoutineActionItem>?): String {
        if (actions == null) return "[]"
        val array = JSONArray()
        for (action in actions) {
            array.put(action.toJsonObject())
        }
        return array.toString()
    }

    @TypeConverter
    fun toActionList(value: String?): List<RoutineActionItem> {
        if (value.isNullOrBlank()) return emptyList()
        val list = mutableListOf<RoutineActionItem>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                list.add(RoutineActionItem.fromJsonObject(array.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // Fallback empty
        }
        return list
    }
}
