package com.example.background

import android.content.Context
import android.util.Log

object WorkManagerHelper {

    private const val TAG = "WorkManagerHelper"

    fun setupPeriodicMonitoring(context: Context) {
        try {
            RoutineWorker.enqueuePeriodicWork(context)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to setup periodic monitoring", e)
        }
    }

    fun triggerImmediateCheck(context: Context) {
        try {
            RoutineWorker.enqueueImmediateWork(context)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to trigger immediate check", e)
        }
    }

    fun triggerRoutineImmediately(context: Context, routineId: Long) {
        try {
            RoutineWorker.enqueueImmediateWork(context, routineId)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to trigger routine immediately", e)
        }
    }
}
