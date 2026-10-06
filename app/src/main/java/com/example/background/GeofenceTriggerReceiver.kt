package com.example.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.PowerManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.service.RoutineExecutionEngine
import com.example.util.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Receives proximity geofence transition intents from Android's LocationManager,
 * verifies precise distance criteria, and executes the routine.
 */
class GeofenceTriggerReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceTriggerReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != GeofenceManager.ACTION_GEOFENCE_TRANSITION) return

        val routineId = intent.getLongExtra(GeofenceManager.EXTRA_ROUTINE_ID, -1L)
        val isEntering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)
        val expectedTransition = intent.getStringExtra(GeofenceManager.EXTRA_TRANSITION_TYPE) ?: "ENTER"

        if (routineId <= 0) {
            Log.w(TAG, "Received geofence transition with invalid routineId: $routineId")
            return
        }

        Log.i(TAG, "Geofence transition received: routineId=$routineId, isEntering=$isEntering, expected=$expectedTransition")

        // Acquire WakeLock to keep CPU active during execution
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PixelRoutines:GeofenceWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(60_000L)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val routine = db.routineDao().getRoutineById(routineId)

                if (routine != null && routine.isEnabled) {
                    val config = JSONObject(routine.triggerConfigJson)
                    val targetLat = config.optDouble("latitude", 0.0)
                    val targetLng = config.optDouble("longitude", 0.0)
                    val radius = config.optInt("radiusMeters", 150)
                    val requiredTransition = config.optString("transition", "ENTER").uppercase()
                    val label = config.optString("label", "Selected Area")

                    val matchesTransition = if (requiredTransition == "ENTER") isEntering else !isEntering

                    if (matchesTransition) {
                        // Double check current GPS distance for maximum precision
                        val currentLocation = LocationHelper.getLastKnownLocation(context)
                        val currentDistance = GeofenceManager.computeDistanceMeters(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            targetLat,
                            targetLng
                        )

                        val isDistanceConsistent = if (requiredTransition == "ENTER") {
                            currentDistance <= (radius * 1.5f) // allow reasonable GPS tolerance
                        } else {
                            currentDistance >= (radius * 0.8f)
                        }

                        if (isDistanceConsistent) {
                            val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000L)
                            if (routine.lastExecutedTimestamp < fiveMinutesAgo) {
                                val triggerReason = "Geolocation: ${if (isEntering) "Arrived at" else "Departed from"} \"$label\" (${currentDistance.toInt()}m away)"
                                val engine = RoutineExecutionEngine(context)
                                val success = engine.executeRoutine(routine, triggerReason)
                                Log.i(TAG, "Geofence routine \"${routine.name}\" executed, success=$success")
                            } else {
                                Log.i(TAG, "Geofence routine \"${routine.name}\" skipped due to recent execution debounce")
                            }
                        } else {
                            Log.w(TAG, "Geofence distance check failed: actual ${currentDistance}m vs radius ${radius}m")
                        }
                    }
                } else {
                    Log.w(TAG, "Routine $routineId not found or disabled. Removing geofence.")
                    GeofenceManager.removeGeofence(context, routineId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing geofence transition", e)
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
