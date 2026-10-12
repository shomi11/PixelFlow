package com.example.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.service.RoutineExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * RoutineAlarmReceiver receives precision AlarmManager intents, executes
 * the privileged routine via RoutineExecutionEngine, and automatically
 * schedules the subsequent alarm instance for recurring daily/weekly triggers.
 */
class RoutineAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RoutineAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == RoutineAlarmManager.ACTION_TRIGGER_CUSTOM_NOTIFICATION) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val routineId = intent.getLongExtra(RoutineAlarmManager.EXTRA_ROUTINE_ID, -1L)
                    val title = intent.getStringExtra(RoutineAlarmManager.EXTRA_NOTIF_TITLE) ?: "Reminder"
                    val message = intent.getStringExtra(RoutineAlarmManager.EXTRA_NOTIF_MESSAGE) ?: ""
                    val iconName = intent.getStringExtra(RoutineAlarmManager.EXTRA_NOTIF_ICON) ?: "bell"
                    val scheduleType = intent.getStringExtra(RoutineAlarmManager.EXTRA_NOTIF_SCHEDULE_TYPE) ?: "ONCE"
                    val time = intent.getStringExtra(RoutineAlarmManager.EXTRA_NOTIF_TIME) ?: "09:00"
                    val index = intent.getIntExtra(RoutineAlarmManager.EXTRA_NOTIF_INDEX, 0)

                    Log.i(TAG, "Custom notification alarm fired: title='$title', scheduleType='$scheduleType', time='$time'")
                    com.example.notification.RoutineNotificationManager.postCustomNotification(
                        context = appContext,
                        title = title,
                        message = message,
                        iconName = iconName
                    )

                    // If daily, reschedule for tomorrow; a "Once" notification must not be re-armed
                    if (scheduleType.equals("DAILY", ignoreCase = true)) {
                        RoutineAlarmManager.scheduleCustomNotificationAlarm(
                            context = appContext,
                            routineId = routineId,
                            title = title,
                            message = message,
                            iconName = iconName,
                            scheduleType = scheduleType,
                            time = time,
                            index = index
                        )
                    } else {
                        RoutineAlarmManager.markCustomNotificationOnceDone(
                            appContext,
                            RoutineAlarmManager.customNotificationOnceKey(routineId, index, title, time)
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling custom notification alarm", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (action != RoutineAlarmManager.ACTION_TRIGGER_ROUTINE_ALARM &&
            action != RoutineAlarmManager.ACTION_RESTORE_ROUTINE &&
            action != RoutineAlarmManager.ACTION_END_ROUTINE_ALARM) return

        val routineId = intent.getLongExtra(RoutineAlarmManager.EXTRA_ROUTINE_ID, -1L)
        val isTest = intent.getBooleanExtra(RoutineAlarmManager.EXTRA_IS_TEST, false)

        if (routineId <= 0) {
            Log.w(TAG, "Received alarm with invalid routineId: $routineId")
            return
        }

        Log.i(TAG, "AlarmManager triggered action $action for routine ID: $routineId (isTest=$isTest)")

        // Hold a partial wake lock to guarantee CPU stays active while executing async actions
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PixelRoutines:RoutineAlarmWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(60_000L) // 60 seconds safety timeout
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val routine = db.routineDao().getRoutineById(routineId)
                val engine = RoutineExecutionEngine(appContext)

                if (routine != null) {
                    when (action) {
                        RoutineAlarmManager.ACTION_RESTORE_ROUTINE -> {
                            Log.i(TAG, "Restoring routine ${routine.name} requested by user action")
                            engine.restoreRoutineSettings(routine, "Manually Ended by User")
                        }
                        RoutineAlarmManager.ACTION_END_ROUTINE_ALARM -> {
                            Log.i(TAG, "Routine schedule end alarm triggered for ${routine.name}")
                            if (routine.restoreOnEnd) {
                                engine.restoreRoutineSettings(routine, "Scheduled time window ended")
                            }
                            if (routine.isEnabled) {
                                RoutineAlarmManager.scheduleExactRoutineAlarm(appContext, routine)
                            }
                        }
                        RoutineAlarmManager.ACTION_TRIGGER_ROUTINE_ALARM -> {
                            if (routine.isEnabled || isTest) {
                                val triggerReason = if (isTest) {
                                    "Precision Alarm Test (Immediate)"
                                } else {
                                    "AlarmManager Scheduled Trigger (${routine.getTriggerSummary()})"
                                }

                                val success = engine.executeRoutine(routine, triggerReason)
                                Log.i(TAG, "Routine executed via precision alarm: ${routine.name}, success=$success")

                                // Reschedule next recurring alarm instance
                                if (!isTest && routine.isEnabled) {
                                    RoutineAlarmManager.scheduleExactRoutineAlarm(appContext, routine)
                                }
                            } else {
                                Log.w(TAG, "Routine $routineId disabled. Canceling alarm.")
                                RoutineAlarmManager.cancelRoutineAlarm(appContext, routineId)
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Routine $routineId not found. Canceling alarm.")
                    RoutineAlarmManager.cancelRoutineAlarm(appContext, routineId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing routine action from alarm", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Error releasing wake lock", e)
                }
                com.example.widget.WidgetUpdateHelper.updateAllWidgets(appContext)
                pendingResult.finish()
            }
        }
    }
}
