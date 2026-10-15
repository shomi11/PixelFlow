package com.vpmapps.pixelflow.background

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import java.util.Calendar

/**
 * Snapshot of the device state that routine conditions are checked against.
 *
 * @param bluetoothDevices connected devices as "name|address" entries
 * @param geofenceInside routine id -> whether the device is inside that routine's location
 */
data class DeviceFacts(
    val wifiConnected: Boolean = false,
    val wifiSsid: String = "",
    val batteryPct: Int = -1,
    val isCharging: Boolean = false,
    val bluetoothDevices: Set<String> = emptySet(),
    val foregroundApp: String? = null,
    val geofenceInside: Map<Long, Boolean> = emptyMap(),
    val now: Calendar = Calendar.getInstance()
)

/**
 * Remembers the device state that can't be read on demand (connected Bluetooth devices, the app in
 * the foreground, geofence results, the Wi-Fi name seen by the monitor service) and combines it with
 * what can be read live (battery, Wi-Fi connection).
 */
object DeviceFactsStore {

    private const val PREFS = "device_facts"
    private const val KEY_BT_DEVICES = "bluetooth_devices"
    private const val KEY_FOREGROUND_APP = "foreground_app"
    private const val KEY_WIFI_CONNECTED = "wifi_connected"
    private const val KEY_WIFI_SSID = "wifi_ssid"
    private const val GEOFENCE_PREFIX = "geofence_inside_"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun snapshot(context: Context): DeviceFacts {
        val prefs = prefs(context)
        val battery = context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else -1
        val plugged = (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0

        val (liveConnected, liveSsid) = readLiveWifi(context)
        val storedSsid = if (prefs.getBoolean(KEY_WIFI_CONNECTED, false)) prefs.getString(KEY_WIFI_SSID, "").orEmpty() else ""

        val geofences = prefs.all.mapNotNull { (key, value) ->
            if (key.startsWith(GEOFENCE_PREFIX) && value is Boolean) {
                key.removePrefix(GEOFENCE_PREFIX).toLongOrNull()?.let { it to value }
            } else {
                null
            }
        }.toMap()

        return DeviceFacts(
            wifiConnected = liveConnected,
            wifiSsid = if (liveConnected) liveSsid.ifBlank { storedSsid } else "",
            batteryPct = pct,
            isCharging = plugged,
            bluetoothDevices = prefs.getStringSet(KEY_BT_DEVICES, emptySet()).orEmpty().toSet(),
            foregroundApp = prefs.getString(KEY_FOREGROUND_APP, null),
            geofenceInside = geofences
        )
    }

    fun setWifi(context: Context, connected: Boolean, ssid: String) {
        prefs(context).edit().putBoolean(KEY_WIFI_CONNECTED, connected).putString(KEY_WIFI_SSID, ssid).apply()
    }

    fun setBluetoothConnected(context: Context, name: String, address: String, connected: Boolean) {
        val prefs = prefs(context)
        val entry = "$name|$address"
        val current = prefs.getStringSet(KEY_BT_DEVICES, emptySet()).orEmpty()
            .filterNot { it.substringAfter('|') == address }
            .toMutableSet()
        if (connected) current.add(entry)
        prefs.edit().putStringSet(KEY_BT_DEVICES, current).apply()
    }

    /** Bluetooth connections don't survive a reboot; devices report themselves again on reconnect. */
    fun clearBluetooth(context: Context) {
        prefs(context).edit().remove(KEY_BT_DEVICES).apply()
    }

    fun setForegroundApp(context: Context, packageName: String) {
        prefs(context).edit().putString(KEY_FOREGROUND_APP, packageName).apply()
    }

    fun setGeofenceInside(context: Context, routineId: Long, inside: Boolean) {
        prefs(context).edit().putBoolean(GEOFENCE_PREFIX + routineId, inside).apply()
    }

    fun clearGeofence(context: Context, routineId: Long) {
        prefs(context).edit().remove(GEOFENCE_PREFIX + routineId).apply()
    }

    @Suppress("DEPRECATION")
    private fun readLiveWifi(context: Context): Pair<Boolean, String> {
        return try {
            val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            if (!onWifi) return false to ""
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ssid = wifiManager?.connectionInfo?.ssid
                ?.takeUnless { it == WifiManager.UNKNOWN_SSID }
                ?.trim('"')
                .orEmpty()
            true to ssid
        } catch (e: Exception) {
            false to ""
        }
    }
}

/**
 * Which routines are currently "on" because their conditions are met. A routine starts when it
 * turns on and ends (restoring settings if enabled) when it turns off.
 */
object RoutineActivationStore {

    private const val PREFS = "routine_activation"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isActive(context: Context, routineId: Long): Boolean =
        prefs(context).getBoolean("active_$routineId", false)

    fun setActive(context: Context, routineId: Long, active: Boolean) {
        prefs(context).edit().putBoolean("active_$routineId", active).apply()
    }

    fun clear(context: Context, routineId: Long) {
        prefs(context).edit().remove("active_$routineId").apply()
    }
}
