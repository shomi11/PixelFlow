package com.vpmapps.pixelflow.background

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import com.vpmapps.pixelflow.data.local.AppDatabase
import com.vpmapps.pixelflow.data.model.Routine
import com.vpmapps.pixelflow.data.model.TriggerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Registers location routines with the Google Play services Geofencing API. Geofences are kept
 * and evaluated on the device by Play services; each one reports both entering and leaving so
 * [TriggerEvaluator] always knows whether the device is inside the routine's area.
 *
 * Earlier versions used LocationManager proximity alerts, which Android only checks every
 * 30 minutes while the app is in the background.
 */
object GeofenceManager {
    private const val TAG = "GeofenceManager"
    const val ACTION_GEOFENCE_TRANSITION = "com.vpmapps.pixelflow.ACTION_GEOFENCE_TRANSITION"

    // One PendingIntent for every geofence; Play services adds which ones fired as extras.
    private const val GEOFENCE_REQUEST_CODE = 69_999

    // Request codes and extras of the per-routine proximity alerts from earlier versions
    private const val LEGACY_REQUEST_CODE_OFFSET = 70_000
    private const val LEGACY_EXTRA_ROUTINE_ID = "extra_routine_id"

    /**
     * Registers (or replaces) the geofence for the given routine.
     */
    @SuppressLint("MissingPermission")
    fun registerGeofence(context: Context, routine: Routine) {
        val condition = routine.condition(TriggerType.LOCATION)
        if (!routine.isEnabled || condition == null) {
            removeGeofence(context, routine.id)
            return
        }

        // Play services needs precise location; on Android 10+ it also refuses geofences
        // without background location (re-registered once the dashboard gets it).
        if (!hasFineLocation(context)) {
            Log.w(TAG, "Cannot register geofence for \"${routine.name}\": precise location not granted")
            return
        }

        try {
            val json = JSONObject(condition.configJson)
            val lat = json.optDouble("latitude", 0.0)
            val lng = json.optDouble("longitude", 0.0)
            val radius = json.optDouble("radiusMeters", 150.0).toFloat()
            val transition = json.optString("transition", "ENTER") // "ENTER" or "EXIT"

            if (lat == 0.0 && lng == 0.0) {
                Log.w(TAG, "Invalid coordinates for routine ${routine.name}")
                return
            }

            val geofence = Geofence.Builder()
                .setRequestId(routine.id.toString())
                .setCircularRegion(lat, lng, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
            // Report the current side right away so the routine doesn't wait for the first crossing
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofence(geofence)
                .build()

            // The area may have changed; re-estimate inside/outside until Play services reports it
            DeviceFactsStore.clearGeofence(context, routine.id)
            // Adding a geofence with an existing request id replaces it
            LocationServices.getGeofencingClient(context)
                .addGeofences(request, geofencePendingIntent(context))
                .addOnSuccessListener {
                    Log.i(TAG, "Registered geofence for \"${routine.name}\" (ID: ${routine.id}) at ($lat, $lng, r=${radius}m, on=$transition)")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register geofence for \"${routine.name}\": ${describe(e)}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register geofence for routine ${routine.name}", e)
        }
    }

    /**
     * Removes the geofence associated with the given routine ID.
     */
    fun removeGeofence(context: Context, routineId: Long) {
        try {
            LocationServices.getGeofencingClient(context)
                .removeGeofences(listOf(routineId.toString()))
                .addOnSuccessListener { Log.i(TAG, "Removed geofence for routine ID: $routineId") }
                .addOnFailureListener { e -> Log.w(TAG, "Error removing geofence for routine ID $routineId: ${describe(e)}") }
        } catch (e: Exception) {
            Log.w(TAG, "Error removing geofence for routine ID: $routineId", e)
        }
        removeLegacyProximityAlert(context, routineId)
    }

    /**
     * Registers all active location routines stored in the Room database. Play services forgets
     * geofences after a reboot, an app update or when location is turned off, so this runs on
     * every app start and boot.
     */
    fun registerAllGeofences(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val routines = AppDatabase.getDatabase(appContext).routineDao().getAllRoutinesList()
                // Start from a clean slate so geofences of deleted routines don't linger
                try {
                    Tasks.await(LocationServices.getGeofencingClient(appContext).removeGeofences(geofencePendingIntent(appContext)))
                } catch (e: Exception) {
                    Log.w(TAG, "Error clearing geofences before re-registering", e)
                }
                var registered = 0
                for (routine in routines) {
                    removeLegacyProximityAlert(appContext, routine.id)
                    if (routine.isEnabled && routine.hasCondition(TriggerType.LOCATION)) {
                        registerGeofence(appContext, routine)
                        registered++
                    }
                }
                Log.i(TAG, "Registering $registered geofences (${routines.size} routines checked)")
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

    private fun hasFineLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun describe(e: Exception): String =
        if (e is ApiException) GeofenceStatusCodes.getStatusCodeString(e.statusCode) else e.toString()

    private fun geofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceTriggerReceiver::class.java).apply {
            action = ACTION_GEOFENCE_TRANSITION
        }
        // Must be mutable so Play services can attach the geofencing event
        return PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /** True for a proximity alert broadcast registered by an earlier version. */
    fun isLegacyProximityAlert(intent: Intent): Boolean =
        intent.hasExtra(LocationManager.KEY_PROXIMITY_ENTERING)

    fun legacyRoutineId(intent: Intent): Long = intent.getLongExtra(LEGACY_EXTRA_ROUTINE_ID, -1L)

    /**
     * Removes the LocationManager proximity alert an earlier version may have left for this
     * routine, so it doesn't report crossings alongside the Play services geofence.
     */
    @SuppressLint("MissingPermission")
    fun removeLegacyProximityAlert(context: Context, routineId: Long) {
        val intent = Intent(context, GeofenceTriggerReceiver::class.java).apply {
            action = ACTION_GEOFENCE_TRANSITION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (routineId + LEGACY_REQUEST_CODE_OFFSET).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_MUTABLE
        ) ?: return
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            locationManager?.removeProximityAlert(pendingIntent)
            Log.i(TAG, "Removed legacy proximity alert for routine ID: $routineId")
        } catch (e: Exception) {
            Log.w(TAG, "Error removing legacy proximity alert for routine ID: $routineId", e)
        } finally {
            pendingIntent.cancel()
        }
    }
}
