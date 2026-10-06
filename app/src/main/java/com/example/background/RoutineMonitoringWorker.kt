package com.example.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Backward compatibility wrapper delegating to RoutineWorker.
 */
class RoutineMonitoringWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val delegate = RoutineWorker(appContext, workerParams)

    override suspend fun doWork(): Result {
        return delegate.doWork()
    }
}
