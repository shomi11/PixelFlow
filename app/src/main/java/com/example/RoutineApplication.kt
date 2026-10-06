package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.example.background.RoutineAlarmManager
import com.example.background.RoutineWorker
import com.example.shizuku.NetworkStatusMonitor
import com.example.shizuku.ShizukuManager

/**
 * RoutineApplication is the custom Application class that initializes WorkManager
 * with an explicit Configuration and immediately schedules a PeriodicWorkRequest
 * for routine observation and execution, ensuring proper background constraints
 * and exponential backoff retries.
 */
class RoutineApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "RoutineApplication"
    }

    /**
     * Custom WorkManager Configuration. Provides logging and worker execution parameters.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RoutineApplication initialized, setting up WorkManager and background schedulers")

        // 1. Initialize WorkManager with custom Configuration if not already initialized
        try {
            if (!WorkManager.isInitialized()) {
                WorkManager.initialize(this, workManagerConfiguration)
            }
            // 2. Schedule the routine observation PeriodicWorkRequest with constraints and retries
            scheduleRoutineObservationWork()
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing WorkManager in RoutineApplication", e)
        }

        // 3. Initialize background helpers: Shizuku bridge, Network monitor, and precision alarms
        try {
            ShizukuManager.init()
            NetworkStatusMonitor.initialize(this)
            RoutineAlarmManager.rescheduleAllRoutines(this)
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing background helpers in RoutineApplication", e)
        }
    }

    /**
     * Enqueues the periodic work request with background constraints and exponential backoff retry criteria.
     */
    private fun scheduleRoutineObservationWork() {
        try {
            val periodicWorkRequest = RoutineWorker.createPeriodicWorkRequest()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                RoutineWorker.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
            Log.i(TAG, "Successfully enqueued PeriodicWorkRequest for routine scheduling with UPDATE policy")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to schedule routine observation work", e)
        }
    }
}
