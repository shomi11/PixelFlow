package com.vpmapps.pixelflow.background

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
import com.vpmapps.pixelflow.data.local.AppDatabase
import com.vpmapps.pixelflow.data.model.ActionType
import com.vpmapps.pixelflow.data.model.Routine
import com.vpmapps.pixelflow.data.model.RoutineActionItem
import com.vpmapps.pixelflow.data.model.RoutineExecutionLog
import com.vpmapps.pixelflow.data.model.TriggerType
import com.vpmapps.pixelflow.service.RoutineExecutionEngine
import com.vpmapps.pixelflow.shizuku.ShellResult
import com.vpmapps.pixelflow.shizuku.ShizukuManager
import com.vpmapps.pixelflow.shizuku.ShizukuServiceHelper
import com.vpmapps.pixelflow.util.TimeParser
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
        dao: com.vpmapps.pixelflow.data.local.RoutineDao
    ) {
        Log.i(TAG, "RoutineWorker performing periodic background health check & scheduler maintenance")
        try {
            // Verify and maintain exact AlarmManager alarms for scheduled, sunrise, and sunset routines
            RoutineAlarmManager.rescheduleAllRoutines(appContext)
            // Restart the trigger monitor if the system killed it (may be refused in the background)
            TriggerMonitorService.refresh(appContext)
            // Safety net: catch any condition change whose event was missed (e.g. phone was off)
            TriggerEvaluator.evaluate(appContext)
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
        dao: com.vpmapps.pixelflow.data.local.RoutineDao
    ): Boolean {
        Log.i(TAG, "Triggering routine via RoutineExecutionEngine: ${routine.name} ($triggerReason)")
        val engine = RoutineExecutionEngine(appContext)
        return engine.executeRoutine(routine, triggerReason)
    }
}
