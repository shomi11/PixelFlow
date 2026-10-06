package com.example.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.Routine
import com.example.data.model.TriggerType
import com.example.util.TimeParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * RoutineAlarmManager coordinates high-precision scheduled triggers using Android's
 * AlarmManager (AlarmClock & While-Idle) for specific times, daily schedules, and weekly intervals.
 */
object RoutineAlarmManager {

    private const val TAG = "RoutineAlarmManager"
    const val ACTION_TRIGGER_ROUTINE_ALARM = "com.example.ACTION_TRIGGER_ROUTINE_ALARM"
    const val EXTRA_ROUTINE_ID = "extra_routine_id"
    const val EXTRA_IS_TEST = "extra_is_test"

    /**
     * Checks if exact alarm scheduling is permitted by the system.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    /**
     * Schedules an exact, precision alarm for the given Routine.
     * Uses AlarmManager.setAlarmClock as primary for guaranteed execution through deep Doze,
     * with graceful fallbacks.
     */
    fun scheduleExactRoutineAlarm(context: Context, routine: Routine) {
        if (!routine.isEnabled) {
            cancelRoutineAlarm(context, routine.id)
            return
        }

        if (routine.triggerType != TriggerType.TIME) {
            return
        }

        val nextTriggerMillis = calculateNextTriggerMillis(routine.triggerConfigJson)
        if (nextTriggerMillis == null || nextTriggerMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "No valid future trigger time calculated for routine ${routine.name}")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createAlarmPendingIntent(context, routine.id)

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_ROUTINE_ID", routine.id)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            (routine.id + 20000).toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // AlarmClockInfo is guaranteed by Android OS to wake the CPU even in deep Doze
            val alarmClockInfo = AlarmManager.AlarmClockInfo(nextTriggerMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

            val formatted = formatNextTriggerHumanReadable(nextTriggerMillis)
            Log.i(TAG, "Scheduled AlarmClock for \"${routine.name}\" (ID: ${routine.id}) at $formatted ($nextTriggerMillis)")
        } catch (e: Exception) {
            Log.w(TAG, "setAlarmClock failed, falling back to exact/while-idle", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerMillis,
                        pendingIntent
                    )
                }
            } catch (e2: SecurityException) {
                Log.e(TAG, "Exact alarm permission missing, falling back to inexact alarm", e2)
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextTriggerMillis, pendingIntent)
            } catch (e3: Exception) {
                Log.e(TAG, "Failed to schedule fallback alarm for routine ${routine.name}", e3)
            }
        }
    }

    /**
     * Schedules a quick precision test alarm (fires after delaySeconds).
     */
    fun scheduleTestAlarm(context: Context, routineId: Long, delaySeconds: Long = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + (delaySeconds * 1000L)

        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ROUTINE_ALARM
            putExtra(EXTRA_ROUTINE_ID, routineId)
            putExtra(EXTRA_IS_TEST, true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (routineId + 50000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.i(TAG, "Scheduled quick test alarm in ${delaySeconds}s for routine ID: $routineId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule test alarm", e)
        }
    }

    /**
     * Cancels any pending precision alarm associated with a routine ID.
     */
    fun cancelRoutineAlarm(context: Context, routineId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createAlarmPendingIntent(context, routineId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.i(TAG, "Canceled precision alarm for routine ID: $routineId")
    }

    /**
     * Reschedules alarms for all active routines stored in Room database.
     */
    fun rescheduleAllRoutines(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeRoutines = db.routineDao().getActiveRoutines()
                for (routine in activeRoutines) {
                    if (routine.triggerType == TriggerType.TIME) {
                        scheduleExactRoutineAlarm(context, routine)
                    }
                }
                Log.i(TAG, "Rescheduled all active time routines (${activeRoutines.size} evaluated)")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling all routines", e)
            }
        }
    }

    private fun createAlarmPendingIntent(context: Context, routineId: Long): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ROUTINE_ALARM
            putExtra(EXTRA_ROUTINE_ID, routineId)
        }
        return PendingIntent.getBroadcast(
            context,
            routineId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Calculates the exact epoch timestamp (in millis) for the next scheduled trigger,
     * taking into account specific time of day, active days of week, and interval repetitions.
     */
    fun calculateNextTriggerMillis(configJson: String): Long? {
        return try {
            val json = JSONObject(configJson)
            val scheduleMode = json.optString("scheduleMode", "DAILY") // "SPECIFIC_TIME", "DAILY", "WEEKLY", "INTERVAL"
            val startTimeStr = json.optString("startTime", "08:00")
            val daysStr = json.optString("days", "Daily")
            val intervalMinutes = json.optInt("intervalMinutes", 0)

            val now = Calendar.getInstance()

            // Interval mode: repeats every X minutes
            if (scheduleMode == "INTERVAL" && intervalMinutes > 0) {
                return now.timeInMillis + (intervalMinutes * 60 * 1000L)
            }

            // Parse target time using robust TimeParser (supporting 24h, 12h AM/PM, "9pm", "9:00 PM")
            val parsedTime = TimeParser.parseToHourMinute(startTimeStr) ?: Pair(21, 0)
            val targetHour = parsedTime.first
            val targetMinute = parsedTime.second

            // Parse active days
            val activeCalendarDays = parseActiveDays(daysStr)

            // Search next 14 days for the closest matching day/time
            for (dayOffset in 0..14) {
                val candidate = (now.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                    set(Calendar.HOUR_OF_DAY, targetHour)
                    set(Calendar.MINUTE, targetMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // If candidate time is in the future
                if (candidate.timeInMillis > now.timeInMillis) {
                    val candidateDayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
                    if (activeCalendarDays.isEmpty() || activeCalendarDays.contains(candidateDayOfWeek)) {
                        return candidate.timeInMillis
                    }
                }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating next trigger millis", e)
            null
        }
    }

    /**
     * Translates human-readable days string ("Mon, Tue, Fri" or "Daily") to Calendar constants.
     */
    private fun parseActiveDays(daysStr: String): Set<Int> {
        val trimmed = daysStr.trim()
        if (trimmed.equals("Daily", ignoreCase = true) ||
            trimmed.equals("Every Day", ignoreCase = true) ||
            trimmed.isBlank()
        ) {
            return setOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
            )
        }

        val result = mutableSetOf<Int>()
        if (trimmed.contains("Mon", ignoreCase = true)) result.add(Calendar.MONDAY)
        if (trimmed.contains("Tue", ignoreCase = true)) result.add(Calendar.TUESDAY)
        if (trimmed.contains("Wed", ignoreCase = true)) result.add(Calendar.WEDNESDAY)
        if (trimmed.contains("Thu", ignoreCase = true)) result.add(Calendar.THURSDAY)
        if (trimmed.contains("Fri", ignoreCase = true)) result.add(Calendar.FRIDAY)
        if (trimmed.contains("Sat", ignoreCase = true)) result.add(Calendar.SATURDAY)
        if (trimmed.contains("Sun", ignoreCase = true)) result.add(Calendar.SUNDAY)
        return result
    }

    /**
     * Formats epoch timestamp into a friendly human-readable preview.
     */
    fun formatNextTriggerHumanReadable(triggerMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = triggerMillis - now
        if (diff <= 0) return "Due now"

        val diffMinutes = diff / (60 * 1000)
        val diffHours = diffMinutes / 60
        val remainingMinutes = diffMinutes % 60

        val dateFormat = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(triggerMillis))

        val relative = when {
            diffHours > 0 -> "in ${diffHours}h ${remainingMinutes}m"
            else -> "in ${diffMinutes}m"
        }

        return "$formattedDate ($relative)"
    }
}
