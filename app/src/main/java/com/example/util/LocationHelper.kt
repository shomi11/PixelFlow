package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val provider: String = "gps",
    val label: String? = null
)

object LocationHelper {
    private const val TAG = "LocationHelper"
    private const val PREFS_NAME = "location_prefs"
    private const val KEY_LAT = "cached_latitude"
    private const val KEY_LNG = "cached_longitude"
    private const val KEY_LABEL = "cached_label"
    private const val KEY_TIME = "cached_time"

    /**
     * Checks whether location permissions are granted.
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Retrieves the best known location from system LocationManager or cached coordinates.
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(context: Context): UserLocation {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (hasLocationPermission(context)) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                var bestLocation: Location? = null

                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )

                for (provider in providers) {
                    try {
                        if (locationManager.isProviderEnabled(provider)) {
                            val loc = locationManager.getLastKnownLocation(provider)
                            if (loc != null) {
                                if (bestLocation == null || loc.time > bestLocation.time) {
                                    bestLocation = loc
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error checking location from $provider: ${e.message}")
                    }
                }

                if (bestLocation != null) {
                    saveCachedLocation(context, bestLocation.latitude, bestLocation.longitude, null)
                    val label = prefs.getString(KEY_LABEL, null) ?: formatCoordinates(bestLocation.latitude, bestLocation.longitude)
                    return UserLocation(
                        latitude = bestLocation.latitude,
                        longitude = bestLocation.longitude,
                        provider = bestLocation.provider ?: "gps",
                        label = label
                    )
                }
            }
        }

        // Check cached preference
        if (prefs.contains(KEY_LAT) && prefs.contains(KEY_LNG)) {
            val lat = prefs.getFloat(KEY_LAT, 0f).toDouble()
            val lng = prefs.getFloat(KEY_LNG, 0f).toDouble()
            val label = prefs.getString(KEY_LABEL, null) ?: formatCoordinates(lat, lng)
            return UserLocation(latitude = lat, longitude = lng, provider = "cache", label = label)
        }

        // Fallback default based on system TimeZone if no GPS or cache available
        val fallback = getFallbackCoordinatesForTimeZone()
        return UserLocation(
            latitude = fallback.first,
            longitude = fallback.second,
            provider = "timezone_default",
            label = formatCoordinates(fallback.first, fallback.second)
        )
    }

    /**
     * Requests a fresh location update and invokes callback.
     */
    @SuppressLint("MissingPermission")
    fun requestFreshLocation(context: Context, onResult: (UserLocation) -> Unit) {
        if (!hasLocationPermission(context)) {
            onResult(getLastKnownLocation(context))
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onResult(getLastKnownLocation(context))
            return
        }

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var requested = false

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                saveCachedLocation(context, location.latitude, location.longitude, null)
                onResult(
                    UserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        provider = location.provider ?: "gps",
                        label = formatCoordinates(location.latitude, location.longitude)
                    )
                )
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        for (p in providers) {
            try {
                if (locationManager.isProviderEnabled(p)) {
                    locationManager.requestLocationUpdates(
                        p,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                    requested = true
                    break
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed requesting updates from $p", e)
            }
        }

        if (!requested) {
            onResult(getLastKnownLocation(context))
        }
    }

    fun saveCachedLocation(context: Context, lat: Double, lng: Double, label: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(KEY_LAT, lat.toFloat())
            putFloat(KEY_LNG, lng.toFloat())
            if (label != null) {
                putString(KEY_LABEL, label)
            }
            putLong(KEY_TIME, System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Attempts reverse-geocoding to display a user-friendly city/area name.
     */
    suspend fun resolveLocationName(context: Context, lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    val country = addr.countryCode
                    if (city != null) {
                        val name = if (country != null) "$city, $country" else city
                        saveCachedLocation(context, lat, lng, name)
                        return@withContext name
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder reverse geocoding failed: ${e.message}")
        }
        formatCoordinates(lat, lng)
    }

    fun formatCoordinates(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return String.format(Locale.US, "%.2f° %s, %.2f° %s", Math.abs(lat), latDir, Math.abs(lng), lngDir)
    }

    /**
     * Fallback approximate coordinates according to the user's timezone if GPS is uninitialized.
     */
    private fun getFallbackCoordinatesForTimeZone(): Pair<Double, Double> {
        val tzId = TimeZone.getDefault().id.lowercase()
        return when {
            tzId.contains("los_angeles") || tzId.contains("pacific") -> Pair(37.7749, -122.4194) // San Francisco
            tzId.contains("new_york") || tzId.contains("eastern") -> Pair(40.7128, -74.0060) // New York
            tzId.contains("chicago") || tzId.contains("central") -> Pair(41.8781, -87.6298) // Chicago
            tzId.contains("denver") || tzId.contains("mountain") -> Pair(39.7392, -104.9903) // Denver
            tzId.contains("london") || tzId.contains("europe/london") -> Pair(51.5074, -0.1278) // London
            tzId.contains("berlin") || tzId.contains("paris") || tzId.contains("rome") -> Pair(52.5200, 13.4050) // Berlin
            tzId.contains("tokyo") || tzId.contains("japan") -> Pair(35.6762, 139.6503) // Tokyo
            tzId.contains("sydney") || tzId.contains("australia") -> Pair(-33.8688, 151.2093) // Sydney
            tzId.contains("belgrade") || tzId.contains("zagreb") || tzId.contains("sarajevo") -> Pair(44.7866, 20.4489) // Belgrade
            else -> Pair(40.0, -75.0)
        }
    }
}
