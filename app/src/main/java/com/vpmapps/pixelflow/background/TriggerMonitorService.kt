package com.vpmapps.pixelflow.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.vpmapps.pixelflow.MainActivity
import com.vpmapps.pixelflow.R
import com.vpmapps.pixelflow.data.local.AppDatabase
import com.vpmapps.pixelflow.data.model.TriggerType
import com.vpmapps.pixelflow.util.SpecialAccessHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that watches for Battery, Wi-Fi and "App opened" triggers.
 *
 * Android 8+ stopped delivering ACTION_POWER_CONNECTED, BATTERY_LOW and Wi-Fi state broadcasts to
 * manifest receivers, and there is no system broadcast for app launches at all, so these triggers
 * only work while a process is alive to listen. The service runs only while at least one enabled
 * routine uses one of these triggers; [refresh] starts or stops it accordingly.
 */
class TriggerMonitorService : Service() {

    companion object {
        private const val TAG = "TriggerMonitorService"
        private const val CHANNEL_ID = "trigger_monitor_channel"
        private const val NOTIFICATION_ID = 4242
        private const val ACTION_RELOAD = "com.vpmapps.pixelflow.background.ACTION_RELOAD_TRIGGERS"

        private val MONITORED_TYPES = setOf(TriggerType.BATTERY, TriggerType.WIFI, TriggerType.APP)

        /**
         * Starts the service if any enabled routine needs it, stops it otherwise. Safe to call from
         * anywhere; starting can be refused while the app is in the background (Android 12+), in
         * which case the next foreground launch, boot, or app update starts it.
         */
        fun refresh(context: Context) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                val needed = try {
                    AppDatabase.getDatabase(appContext).routineDao().getActiveRoutines()
                        .any { routine -> routine.triggers.any { it.type in MONITORED_TYPES } }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check monitored routines", e)
                    return@launch
                }
                val intent = Intent(appContext, TriggerMonitorService::class.java).setAction(ACTION_RELOAD)
                if (needed) {
                    try {
                        ContextCompat.startForegroundService(appContext, intent)
                    } catch (e: Exception) {
                        // ForegroundServiceStartNotAllowedException (API 31+) when started from the background
                        Log.w(TAG, "Could not start trigger monitor from the background yet", e)
                    }
                } else {
                    appContext.stopService(intent)
                }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var batteryReceiver: BroadcastReceiver? = null
    private var wifiCallback: ConnectivityManager.NetworkCallback? = null
    private var appWatchJob: Job? = null

    private var lastBatteryState: Pair<Int, Boolean>? = null
    private val wifiNetworks = mutableMapOf<Network, String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Every startForegroundService() call must be answered with startForeground().
        startInForeground()
        scope.launch { reloadMonitors() }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterBattery()
        unregisterWifi()
        appWatchJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        createChannel()
        val openIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_bolt)
            .setContentTitle("Routines are active")
            .setContentText("Watching for Wi-Fi, battery and app triggers")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            stopSelf()
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "Background triggers", NotificationManager.IMPORTANCE_MIN).apply {
            description = "Shown while PixelFlow watches for Wi-Fi, battery and app triggers"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private suspend fun reloadMonitors() {
        val types = try {
            AppDatabase.getDatabase(this).routineDao().getActiveRoutines().flatMap { routine -> routine.triggers.map { it.type } }.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading routines", e)
            return
        }
        if (types.none { it in MONITORED_TYPES }) {
            Log.i(TAG, "No Wi-Fi, battery or app routines are enabled; stopping")
            stopSelf()
            return
        }

        if (TriggerType.BATTERY in types) registerBattery() else unregisterBattery()
        if (TriggerType.WIFI in types) registerWifi() else unregisterWifi()
        if (TriggerType.APP in types) startAppWatch() else stopAppWatch()
    }

    // --- Battery ---

    @Synchronized
    private fun registerBattery() {
        if (batteryReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val sticky = if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    intent
                } else {
                    context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                }
                val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else -1
                val isCharging = when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> true
                    Intent.ACTION_POWER_DISCONNECTED -> false
                    else -> (sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
                }
                if (pct < 0) return

                val state = pct to isCharging
                if (state == lastBatteryState) return
                lastBatteryState = state
                scope.launch { TriggerEvaluator.onBatteryState(applicationContext, pct, isCharging) }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        // System broadcasts only, so the receiver doesn't need to be exported.
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        batteryReceiver = receiver
        Log.i(TAG, "Battery monitoring started")
    }

    @Synchronized
    private fun unregisterBattery() {
        batteryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // Already unregistered
            }
        }
        batteryReceiver = null
        lastBatteryState = null
    }

    // --- Wi-Fi ---

    @Synchronized
    private fun registerWifi() {
        if (wifiCallback != null) return
        val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // FLAG_INCLUDE_LOCATION_INFO is required to read the SSID on Android 12+
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = onWifiCapabilities(network, caps)
                override fun onLost(network: Network) = onWifiLost(network)
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = onWifiCapabilities(network, caps)
                override fun onLost(network: Network) = onWifiLost(network)
            }
        }
        try {
            connectivityManager.registerNetworkCallback(request, callback)
            wifiCallback = callback
            Log.i(TAG, "Wi-Fi monitoring started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register Wi-Fi callback", e)
        }
    }

    private fun onWifiCapabilities(network: Network, caps: NetworkCapabilities) {
        val ssid = readSsid(caps)
        val changed = synchronized(wifiNetworks) {
            val previous = wifiNetworks[network]
            // Keep a known SSID if a later update temporarily reports it as unknown
            val resolved = ssid.ifBlank { previous.orEmpty() }
            wifiNetworks[network] = resolved
            previous != resolved
        }
        if (changed) {
            val current = synchronized(wifiNetworks) { wifiNetworks[network].orEmpty() }
            scope.launch { TriggerEvaluator.onWifiState(applicationContext, isConnected = true, ssid = current) }
        }
    }

    private fun onWifiLost(network: Network) {
        val stillConnected = synchronized(wifiNetworks) {
            wifiNetworks.remove(network)
            wifiNetworks.values.firstOrNull()
        }
        scope.launch {
            if (stillConnected != null) {
                TriggerEvaluator.onWifiState(applicationContext, isConnected = true, ssid = stillConnected)
            } else {
                TriggerEvaluator.onWifiState(applicationContext, isConnected = false, ssid = "")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun readSsid(caps: NetworkCapabilities): String {
        val fromCaps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (caps.transportInfo as? WifiInfo)?.ssid
        } else {
            null
        }
        val raw = fromCaps?.takeUnless { it == WifiManager.UNKNOWN_SSID }
            ?: try {
                (applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.connectionInfo?.ssid
            } catch (e: Exception) {
                null
            }
        return raw?.takeUnless { it == WifiManager.UNKNOWN_SSID }?.trim('"').orEmpty()
    }

    @Synchronized
    private fun unregisterWifi() {
        wifiCallback?.let {
            try {
                getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                // Already unregistered
            }
        }
        wifiCallback = null
        synchronized(wifiNetworks) { wifiNetworks.clear() }
    }

    // --- App opened ---

    @Synchronized
    private fun startAppWatch() {
        if (appWatchJob?.isActive == true) return
        appWatchJob = scope.launch {
            if (!SpecialAccessHelper.hasUsageAccess(applicationContext)) {
                SpecialAccessHelper.grantUsageAccessViaShizuku(applicationContext)
            }
            val usageStats = getSystemService(UsageStatsManager::class.java) ?: return@launch
            val powerManager = getSystemService(PowerManager::class.java)
            var foregroundPackage: String? = null
            var warnedNoAccess = false
            Log.i(TAG, "App launch monitoring started")

            while (isActive) {
                if (powerManager?.isInteractive == false) {
                    delay(5_000)
                    continue
                }
                if (!SpecialAccessHelper.hasUsageAccess(applicationContext)) {
                    if (!warnedNoAccess) {
                        Log.w(TAG, "Usage access not granted; \"App opened\" routines can't run")
                        warnedNoAccess = true
                    }
                    SpecialAccessHelper.grantUsageAccessViaShizuku(applicationContext)
                    delay(30_000)
                    continue
                }
                warnedNoAccess = false

                val latest = latestResumedPackage(usageStats)
                if (latest != null && latest != foregroundPackage) {
                    val previous = foregroundPackage
                    foregroundPackage = latest
                    TriggerEvaluator.onForegroundAppChanged(applicationContext, previous, latest)
                }
                delay(1_500)
            }
        }
    }

    @Synchronized
    private fun stopAppWatch() {
        appWatchJob?.cancel()
        appWatchJob = null
    }

    private fun latestResumedPackage(usageStats: UsageStatsManager): String? {
        val now = System.currentTimeMillis()
        val events = try {
            usageStats.queryEvents(now - 10_000, now)
        } catch (e: Exception) {
            return null
        }
        val event = UsageEvents.Event()
        var latest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                latest = event.packageName
            }
        }
        return latest
    }
}
