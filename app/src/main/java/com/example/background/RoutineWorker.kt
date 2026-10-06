package com.example.background

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.AppDatabase
import com.example.data.model.ActionType
import com.example.data.model.Routine
import com.example.data.model.RoutineActionItem
import com.example.data.model.RoutineExecutionLog
import com.example.data.model.TriggerType
import com.example.service.RoutineExecutionEngine
import com.example.shizuku.ShellResult
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuServiceHelper
import com.example.util.TimeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * RoutineWorker extends CoroutineWorker to observe routine execution schedules
 * and trigger privileged Shizuku shell commands based on configurations stored in Room.
 */
class RoutineWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "RoutineWorker"
        const val PERIODIC_WORK_NAME = "RoutineWorker_Periodic"
        const val KEY_ROUTINE_ID = "key_routine_id"

        /**
         * Builds the standardized PeriodicWorkRequest with proper background constraints,
         * flex interval, and exponential backoff retry criteria.
         */
        fun createPeriodicWorkRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build()

            return PeriodicWorkRequestBuilder<RoutineWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .addTag(PERIODIC_WORK_NAME)
                .build()
        }

        /**
         * Enqueues periodic routine observation via WorkManager.
         */
        fun enqueuePeriodicWork(context: Context) {
            try {
                val periodicRequest = createPeriodicWorkRequest()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicRequest
                )
                Log.i(TAG, "Enqueued periodic RoutineWorker observation with constraints and exponential backoff")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to enqueue periodic RoutineWorker", e)
            }
        }

        /**
         * Enqueues an immediate run of RoutineWorker, optionally for a specific routine.
         */
        fun enqueueImmediateWork(context: Context, routineId: Long = -1L) {
            try {
                val builder = OneTimeWorkRequestBuilder<RoutineWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                if (routineId > 0) {
                    builder.setInputData(workDataOf(KEY_ROUTINE_ID to routineId))
                }
                val request = builder.build()
                val uniqueName = if (routineId > 0) "RoutineImmediate_$routineId" else "RoutineImmediate_All"

                WorkManager.getInstance(context).enqueueUniqueWork(
                    uniqueName,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to enqueue immediate RoutineWorker", e)
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "RoutineWorker executing routine schedule observation (attempt: $runAttemptCount)")
        val database = AppDatabase.getDatabase(appContext)
        val dao = database.routineDao()

        val specificRoutineId = inputData.getLong(KEY_ROUTINE_ID, -1L)

        try {
            if (specificRoutineId > 0) {
                val routine = dao.getRoutineById(specificRoutineId)
                if (routine != null && routine.isEnabled) {
                    val success = executeRoutineWithShizuku(routine, "Immediate Execution Trigger", dao)
                    if (!success && runAttemptCount < 3) {
                        Log.w(TAG, "Execution failed for routine $specificRoutineId, requesting retry ($runAttemptCount/3)")
                        return@withContext Result.retry()
                    }
                }
            } else {
                observeAndExecuteScheduledRoutines(dao)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing RoutineWorker (attempt: $runAttemptCount)", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Performs background scheduler verification and health checks.
     * Prevents spurious periodic executions while ensuring hardware alarms and geofences remain active.
     */
    private suspend fun observeAndExecuteScheduledRoutines(
        dao: com.example.data.local.RoutineDao
    ) {
        Log.i(TAG, "RoutineWorker performing periodic background health check & scheduler maintenance")
        try {
            // Verify and maintain exact AlarmManager alarms for scheduled, sunrise, and sunset routines
            RoutineAlarmManager.rescheduleAllRoutines(appContext)
            // Verify and maintain hardware geofence proximity alerts for active geolocation routines
            GeofenceManager.registerAllGeofences(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing background scheduler maintenance in RoutineWorker", e)
        }
    }

    /**
     * Executes each action of the routine, dispatching privileged shell commands via RoutineExecutionEngine
     * and posting unified completion notifications.
     */
    private suspend fun executeRoutineWithShizuku(
        routine: Routine,
        triggerReason: String,
        dao: com.example.data.local.RoutineDao
    ): Boolean {
        Log.i(TAG, "Triggering routine via RoutineExecutionEngine: ${routine.name} ($triggerReason)")
        val engine = RoutineExecutionEngine(appContext)
        return engine.executeRoutine(routine, triggerReason)
    }

    /**
     * Evaluates whether current time and day of week match the routine's schedule.
     */
    private fun isTimeMatch(routine: Routine, currentMinutes: Int, dayOfWeek: Int): Boolean {
        return try {
            val json = JSONObject(routine.triggerConfigJson)
            val startTimeStr = json.optString("startTime", "22:00")
            val endTimeStr = json.optString("endTime", "07:00")
            val daysStr = json.optString("days", "Daily")

            val dayName = when (dayOfWeek) {
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                Calendar.SUNDAY -> "Sun"
                else -> ""
            }
            if (daysStr != "Daily" && !daysStr.contains(dayName, ignoreCase = true)) {
                return false
            }

            val parsedStart = TimeParser.parseToHourMinute(startTimeStr) ?: return false
            val startMin = parsedStart.first * 60 + parsedStart.second

            val parsedEnd = TimeParser.parseToHourMinute(endTimeStr)
            if (parsedEnd != null && parsedEnd != parsedStart) {
                val endMin = parsedEnd.first * 60 + parsedEnd.second
                if (startMin <= endMin) {
                    currentMinutes in startMin..endMin
                } else {
                    currentMinutes >= startMin || currentMinutes <= endMin
                }
            } else {
                // Point in time trigger: match if current time is within 15 min window
                val diff = currentMinutes - startMin
                diff in 0..15
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Evaluates battery percentage and charging condition.
     */
    private fun isBatteryMatch(routine: Routine, batteryStatus: BatteryState): Boolean {
        return try {
            val json = JSONObject(routine.triggerConfigJson)
            val reqCharging = json.optBoolean("charging", false)
            val targetLevel = json.optInt("level", 20)

            if (reqCharging) {
                batteryStatus.isCharging
            } else {
                batteryStatus.level <= targetLevel && !batteryStatus.isCharging
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isLocationMatch(routine: Routine, context: Context): Boolean {
        return try {
            val config = JSONObject(routine.triggerConfigJson)
            val targetLat = config.optDouble("latitude", 0.0)
            val targetLng = config.optDouble("longitude", 0.0)
            val radius = config.optInt("radiusMeters", 150)
            val transition = config.optString("transition", "ENTER").uppercase()

            if (targetLat == 0.0 && targetLng == 0.0) return false

            val currentLoc = com.example.util.LocationHelper.getLastKnownLocation(context)
            val distance = GeofenceManager.computeDistanceMeters(
                currentLoc.latitude, currentLoc.longitude, targetLat, targetLng
            )

            if (transition == "ENTER") {
                distance <= radius
            } else {
                distance > radius
            }
        } catch (e: Exception) {
            false
        }
    }

    data class BatteryState(val level: Int, val isCharging: Boolean)

    private fun getBatteryStatus(context: Context): BatteryState {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, filter)
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 50

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryState(pct, isCharging)
    }
}
