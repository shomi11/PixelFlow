package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Robust TimeParser that accommodates 24-hour (e.g. "21:00"), 12-hour AM/PM (e.g. "9pm", "9:00 PM"),
 * and informal shorthand time inputs entered by users.
 */
object TimeParser {

    /**
     * Parses diverse time representations into an (hour, minute) pair (0..23, 0..59).
     * Returns null if unable to resolve a valid time.
     */
    fun parseToHourMinute(input: String?): Pair<Int, Int>? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()

        // 1. Regex capturing: "9", "9pm", "9:00 PM", "21:00", "9:30p", "12am", etc.
        val regex = Regex("""^(\d{1,2})(?::(\d{1,2}))?\s*([aApP][mM]?)?$""")
        val match = regex.find(trimmed)
        if (match != null) {
            val rawHour = match.groupValues[1].toIntOrNull() ?: return null
            val rawMinuteStr = match.groupValues[2]
            val rawMinute = if (rawMinuteStr.isNotBlank()) rawMinuteStr.toIntOrNull() ?: return null else 0
            val amPm = match.groupValues[3].uppercase(Locale.US)

            if (rawMinute !in 0..59) return null

            val resolvedHour = when {
                amPm.startsWith("P") -> {
                    when {
                        rawHour in 1..11 -> rawHour + 12
                        rawHour == 12 -> 12
                        else -> null
                    }
                }
                amPm.startsWith("A") -> {
                    when {
                        rawHour == 12 -> 0
                        rawHour in 1..11 -> rawHour
                        rawHour == 0 -> 0
                        else -> null
                    }
                }
                else -> {
                    // No AM/PM designation: must be 24-hour standard (0..23)
                    if (rawHour in 0..23) rawHour else null
                }
            }

            if (resolvedHour != null) {
                return Pair(resolvedHour, rawMinute)
            }
        }

        // 2. Standard format fallbacks
        val patterns = listOf(
            "HH:mm", "H:mm", "h:mm a", "hh:mm a", "h:mma", "hh:mma", "h a", "ha"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.isLenient = false
                val date = sdf.parse(trimmed)
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                }
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Formats an hour and minute into a canonical 24-hour time string ("HH:mm").
     */
    fun format24Hour(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    /**
     * Formats an hour and minute into a readable 12-hour AM/PM string (e.g. "9:00 PM").
     */
    fun format12Hour(hour: Int, minute: Int): String {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val amPm = if (h >= 12) "PM" else "AM"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, m, amPm)
    }

    /**
     * Returns a human-friendly label combining 12-hour and 24-hour representation.
     * E.g. "9:00 PM (21:00)"
     */
    fun formatFriendlyPreview(input: String?): String {
        val parsed = parseToHourMinute(input) ?: return "Invalid time"
        val twelve = format12Hour(parsed.first, parsed.second)
        val twentyFour = format24Hour(parsed.first, parsed.second)
        return "$twelve ($twentyFour)"
    }
}
