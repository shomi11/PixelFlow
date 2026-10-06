package com.example.background

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.Routine
import com.example.data.model.TriggerType
import com.example.util.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Coordinates high-precision geolocation and geofence triggers using Android's
 * LocationManager Proximity Alerts and exact geographic distance calculations.
 */
object GeofenceManager {
    private const val TAG = "GeofenceManager"
    const val ACTION_GEOFENCE_TRANSITION = "com.example.ACTION_GEOFENCE_TRANSITION"
    const val EXTRA_ROUTINE_ID = "extra_routine_id"
    const val EXTRA_TARGET_LAT = "extra_target_lat"
    const val EXTRA_TARGET_LNG = "extra_target_lng"
    const val EXTRA_RADIUS_METERS = "extra_radius_meters"
    const val EXTRA_TRANSITION_TYPE = "extra_transition_type"

    /**
     * Registers a precision proximity alert geofence for the given routine.
     */
    @SuppressLint("MissingPermission")
    fun registerGeofence(context: Context, routine: Routine) {
        if (!routine.isEnabled || routine.triggerType != TriggerType.LOCATION) {
            removeGeofence(context, routine.id)
            return
        }

        if (!LocationHelper.hasLocationPermission(context)) {
            Log.w(TAG, "Cannot register geofence for \"${routine.name}\": location permission not granted")
            return
        }

        try {
            val json = JSONObject(routine.triggerConfigJson)
            val lat = json.optDouble("latitude", 0.0)
            val lng = json.optDouble("longitude", 0.0)
            val radius = json.optDouble("radiusMeters", 150.0).toFloat()
            val transition = json.optString("transition", "ENTER") // "ENTER" or "EXIT"

            if (lat == 0.0 && lng == 0.0) {
                Log.w(TAG, "Invalid coordinates for routine ${routine.name}")
                return
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val pendingIntent = createGeofencePendingIntent(context, routine.id, lat, lng, radius.toInt(), transition)

            // -1L indicates expiration in infinity (active until removed)
            locationManager.addProximityAlert(lat, lng, radius, -1L, pendingIntent)
            Log.i(TAG, "Registered proximity geofence for \"${routine.name}\" (ID: ${routine.id}) at ($lat, $lng, r=${radius}m, on=$transition)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register geofence for routine ${routine.name}", e)
        }
    }

    /**
     * Removes the proximity alert geofence associated with the given routine ID.
     */
    @SuppressLint("MissingPermission")
    fun removeGeofence(context: Context, routineId: Long) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val pendingIntent = createGeofencePendingIntent(context, routineId, 0.0, 0.0, 0, "ENTER")
            locationManager.removeProximityAlert(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Removed proximity geofence for routine ID: $routineId")
        } catch (e: Exception) {
            Log.w(TAG, "Error removing geofence for routine ID: $routineId", e)
        }
    }

    /**
     * Registers all active geolocation routines stored in the Room database.
     */
    fun registerAllGeofences(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeRoutines = db.routineDao().getActiveRoutines()
                for (routine in activeRoutines) {
                    if (routine.triggerType == TriggerType.LOCATION) {
                        registerGeofence(context, routine)
                    }
                }
                Log.i(TAG, "Evaluated and registered active geofences (${activeRoutines.size} routines evaluated)")
            } catch (e: Exception) {
                Log.e(TAG, "Error registering all geofences", e)
            }
        }
    }

    /**
     * Helper to compute the distance in meters between two coordinates.
     */
    fun computeDistanceMeters(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0]
    }

    private fun createGeofencePendingIntent(
        context: Context,
        routineId: Long,
        lat: Double,
        lng: Double,
        radiusMeters: Int,
        transition: String
    ): PendingIntent {
        val intent = Intent(context, GeofenceTriggerReceiver::class.java).apply {
            action = ACTION_GEOFENCE_TRANSITION
            putExtra(EXTRA_ROUTINE_ID, routineId)
            putExtra(EXTRA_TARGET_LAT, lat)
            putExtra(EXTRA_TARGET_LNG, lng)
            putExtra(EXTRA_RADIUS_METERS, radiusMeters)
            putExtra(EXTRA_TRANSITION_TYPE, transition)
        }
        return PendingIntent.getBroadcast(
            context,
            (routineId + 70000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}
