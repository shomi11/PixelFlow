package com.example.shizuku

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Monitors network interface shifts (Wi-Fi vs Cellular) and battery optimization states
 * to explain and resolve Shizuku wireless debugging disconnections when the user leaves home.
 */
object NetworkStatusMonitor {

    private const val TAG = "NetworkStatusMonitor"

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _isWifiConnected = MutableStateFlow(false)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private val _isCellularConnected = MutableStateFlow(false)
    val isCellularConnected: StateFlow<Boolean> = _isCellularConnected.asStateFlow()

    private val _networkDescription = MutableStateFlow("Detecting network...")
    val networkDescription: StateFlow<String> = _networkDescription.asStateFlow()

    @Volatile
    private var isInitialized = false
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (isInitialized) return
        val app = context.applicationContext
        appContext = app

        val connectivityManager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            Log.w(TAG, "ConnectivityManager unavailable")
            return
        }

        // Initial snapshot
        updateCurrentNetworkState(connectivityManager)

        // Register live network request
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch {
                        updateCurrentNetworkState(connectivityManager)
                    }
                }

                override fun onLost(network: Network) {
                    scope.launch {
                        val wasWifi = _isWifiConnected.value
                        updateCurrentNetworkState(connectivityManager)
                        if (wasWifi && !_isWifiConnected.value) {
                            Log.w(TAG, "Wi-Fi disconnected! Android turns off Wireless Debugging outside Wi-Fi.")
                            ShizukuPermissionManager.recordDiagnostic(
                                level = DiagnosticLevel.WARNING,
                                stage = "Wi-Fi Disconnected",
                                message = "Device left Wi-Fi (switched to mobile data or offline). Android OS automatically stops the Wireless Debugging port when disconnected from Wi-Fi.",
                                technicalDetails = "adb_wifi requires an active Wi-Fi or Hotspot network interface. Cellular connections cannot host adb ports.",
                                suggestedFix = "1. When home: Reconnect to Wi-Fi.\n2. Away from home: Turn on Personal Hotspot to enable Wireless Debugging without a router.\n3. Keep Shizuku battery set to 'Unrestricted'."
                            )
                            ShizukuManager.checkStatus()
                        }
                    }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    scope.launch {
                        val previousWifi = _isWifiConnected.value
                        val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        val hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

                        _isWifiConnected.value = hasWifi
                        _isCellularConnected.value = hasCellular

                        if (!previousWifi && hasWifi) {
                            Log.i(TAG, "Wi-Fi reconnected! Checking Shizuku binder status...")
                            ShizukuPermissionManager.recordDiagnostic(
                                level = DiagnosticLevel.INFO,
                                stage = "Wi-Fi Restored",
                                message = "Connected to Wi-Fi. Checking Shizuku daemon status...",
                                technicalDetails = "Active Wi-Fi connection detected. Wireless Debugging can now connect."
                            )
                            ShizukuManager.checkStatus()
                        }

                        _networkDescription.value = when {
                            hasWifi -> "Connected to Wi-Fi"
                            hasCellular -> "Mobile Data (Cellular)"
                            else -> "No Network Connection"
                        }
                    }
                }
            })
            isInitialized = true
            Log.i(TAG, "NetworkStatusMonitor successfully registered")
        } catch (e: Throwable) {
            Log.e(TAG, "Error registering network callback", e)
        }
    }

    private fun updateCurrentNetworkState(connectivityManager: ConnectivityManager) {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val hasCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        _isWifiConnected.value = hasWifi
        _isCellularConnected.value = hasCellular
        _networkDescription.value = when {
            hasWifi -> "Connected to Wi-Fi"
            hasCellular -> "Mobile Data (Cellular)"
            else -> "Offline"
        }
    }

    /**
     * Checks if Pixel Routines has been exempted from battery optimizations.
     */
    fun isAppBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Opens Android's Hotspot & Tethering settings so the user can activate
     * Wireless Debugging on the go without home Wi-Fi.
     */
    fun openHotspotSettings(context: Context) {
        try {
            val intent = Intent("android.settings.TETHER_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Could not open Hotspot settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Opens the Android App Info details page for the Shizuku Manager application
     * so the user can set Battery usage to "Unrestricted", preventing the OS from
     * killing the background daemon when leaving the house or locking the screen.
     */
    fun openShizukuAppDetails(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", "moe.shizuku.privileged.api", null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Shizuku application details not found", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens Android's Battery Optimization exemption request dialog.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Cannot open battery optimization settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
