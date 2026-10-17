package com.vpmapps.pixelflow.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.vpmapps.pixelflow.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives geofence transitions from Google Play services and records whether the device is
 * inside each routine's area; [TriggerEvaluator] decides whether that starts or ends the routine.
 */
class GeofenceTriggerReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceTriggerReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GeofenceManager.ACTION_GEOFENCE_TRANSITION) return

        if (GeofenceManager.isLegacyProximityAlert(intent)) {
            // A proximity alert left by an earlier version; the Play services geofence replaces it
            GeofenceManager.removeLegacyProximityAlert(context, GeofenceManager.legacyRoutineId(intent))
            return
        }

        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            // GEOFENCE_NOT_AVAILABLE means location was turned off and Play services dropped every
            // geofence; they're added again on the next app start or boot.
            Log.w(TAG, "Geofencing error: ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }
        val inside = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> true
            Geofence.GEOFENCE_TRANSITION_EXIT -> false
            else -> return
        }
        val routineIds = event.triggeringGeofences.orEmpty().mapNotNull { it.requestId.toLongOrNull() }
        Log.i(TAG, "Geofence transition: routineIds=$routineIds, entering=$inside")
        if (routineIds.isEmpty()) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(appContext).routineDao()
                for (routineId in routineIds) {
                    val routine = dao.getRoutineById(routineId)
                    if (routine == null || !routine.isEnabled) {
                        Log.w(TAG, "Routine $routineId not found or disabled. Removing geofence.")
                        GeofenceManager.removeGeofence(appContext, routineId)
                    } else {
                        TriggerEvaluator.onGeofenceChanged(appContext, routineId, inside = inside)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing geofence transition", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
