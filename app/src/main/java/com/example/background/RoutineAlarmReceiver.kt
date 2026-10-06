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
        val action = intent.action
        if (action != RoutineAlarmManager.ACTION_TRIGGER_ROUTINE_ALARM) return

        val routineId = intent.getLongExtra(RoutineAlarmManager.EXTRA_ROUTINE_ID, -1L)
        val isTest = intent.getBooleanExtra(RoutineAlarmManager.EXTRA_IS_TEST, false)

        if (routineId <= 0) {
            Log.w(TAG, "Received alarm with invalid routineId: $routineId")
            return
        }

        Log.i(TAG, "AlarmManager triggered for routine ID: $routineId (isTest=$isTest)")

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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val routine = db.routineDao().getRoutineById(routineId)

                if (routine != null && (routine.isEnabled || isTest)) {
                    val triggerReason = if (isTest) {
                        "Precision Alarm Test (Immediate)"
                    } else {
                        "AlarmManager Scheduled Trigger (${routine.getTriggerSummary()})"
                    }

                    val engine = RoutineExecutionEngine(context)
                    val success = engine.executeRoutine(routine, triggerReason)
                    Log.i(TAG, "Routine executed via precision alarm: ${routine.name}, success=$success")

                    // Reschedule next recurring alarm instance
                    if (!isTest && routine.isEnabled) {
                        RoutineAlarmManager.scheduleExactRoutineAlarm(context, routine)
                    }
                } else {
                    Log.w(TAG, "Routine $routineId not found or disabled. Canceling alarm.")
                    RoutineAlarmManager.cancelRoutineAlarm(context, routineId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing routine from alarm", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Error releasing wake lock", e)
                }
                pendingResult.finish()
            }
        }
    }
}
