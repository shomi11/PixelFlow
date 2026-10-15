package com.vpmapps.pixelflow.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log
import com.vpmapps.pixelflow.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives proximity alerts from LocationManager and records whether the device is inside the
 * routine's area; [TriggerEvaluator] decides whether that starts or ends the routine.
 */
class GeofenceTriggerReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceTriggerReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GeofenceManager.ACTION_GEOFENCE_TRANSITION) return

        val routineId = intent.getLongExtra(GeofenceManager.EXTRA_ROUTINE_ID, -1L)
        if (routineId <= 0) {
            Log.w(TAG, "Received geofence transition with invalid routineId: $routineId")
            return
        }
        val isEntering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)
        Log.i(TAG, "Geofence transition: routineId=$routineId, entering=$isEntering")

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val routine = AppDatabase.getDatabase(appContext).routineDao().getRoutineById(routineId)
                if (routine == null || !routine.isEnabled) {
                    Log.w(TAG, "Routine $routineId not found or disabled. Removing geofence.")
                    GeofenceManager.removeGeofence(appContext, routineId)
                } else {
                    TriggerEvaluator.onGeofenceChanged(appContext, routineId, inside = isEntering)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing geofence transition", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
