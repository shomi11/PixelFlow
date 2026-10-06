package com.example.shizuku

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

enum class DiagnosticLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

/**
 * Diagnostic log item representing a distinct step in Shizuku binder connection,
 * permissions verification, or wireless debugging lifecycle.
 */
data class ShizukuDiagnosticEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: DiagnosticLevel,
    val stage: String,
    val message: String,
    val technicalDetails: String? = null,
    val suggestedFix: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

/**
 * High-performance permission coordinator and dedicated diagnostic logger that
 * directly monitors runtime permission changes via [Shizuku.OnRequestPermissionResultListener]
 * and records detailed binder connection telemetry to debug wireless and IPC failures.
 */
object ShizukuPermissionManager : Shizuku.OnRequestPermissionResultListener {

    private const val TAG = "ShizukuPermissionMgr"
    const val DEFAULT_REQUEST_CODE = 7102
    private const val MAX_DIAGNOSTIC_LOGS = 100

    enum class PermissionStatus {
        UNKNOWN,
        GRANTED,
        DENIED
    }

    data class PermissionResultEvent(
        val requestCode: Int,
        val isGranted: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _isGranted = MutableStateFlow(false)
    val isGranted: StateFlow<Boolean> = _isGranted.asStateFlow()

    private val _permissionStatus = MutableStateFlow(PermissionStatus.UNKNOWN)
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    private val _permissionEvents = MutableSharedFlow<PermissionResultEvent>(extraBufferCapacity = 16)
    val permissionEvents: SharedFlow<PermissionResultEvent> = _permissionEvents.asSharedFlow()

    // Diagnostic logging state flow
    private val rawDiagnostics = CopyOnWriteArrayList<ShizukuDiagnosticEntry>()
    private val _diagnosticLogs = MutableStateFlow<List<ShizukuDiagnosticEntry>>(emptyList())
    val diagnosticLogs: StateFlow<List<ShizukuDiagnosticEntry>> = _diagnosticLogs.asStateFlow()

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    private val _lastReconnectAttempt = MutableStateFlow(0L)
    val lastReconnectAttempt: StateFlow<Long> = _lastReconnectAttempt.asStateFlow()

    @Volatile
    private var isAppLifecycleAttached = false

    private val changeListeners = CopyOnWriteArraySet<(Boolean) -> Unit>()

    @Volatile
    private var isRegistered = false

    private val internalBinderReceivedListener = Shizuku.OnBinderReceivedListener {
        recordDiagnostic(
            level = DiagnosticLevel.SUCCESS,
            stage = "Binder Connection",
            message = "Shizuku IPC binder attached successfully.",
            technicalDetails = "Shizuku.pingBinder() = true. Ready to dispatch commands."
        )
        checkPermission()
    }

    private val internalBinderDeadListener = Shizuku.OnBinderDeadListener {
        recordDiagnostic(
            level = DiagnosticLevel.ERROR,
            stage = "Binder Disconnected",
            message = "Shizuku IPC binder died or disconnected unexpectedly.",
            technicalDetails = "The Shizuku daemon process may have terminated, device rebooted, or Wi-Fi network changed.",
            suggestedFix = "Re-open Shizuku app and start Wireless Debugging service."
        )
        _isGranted.value = false
        _permissionStatus.value = PermissionStatus.UNKNOWN
    }

    /**
     * Initializes and registers the listener with Shizuku safely.
     */
    fun register() {
        if (isRegistered) {
            checkPermission()
            return
        }
        try {
            Shizuku.addRequestPermissionResultListener(this)
            Shizuku.addBinderReceivedListenerSticky(internalBinderReceivedListener)
            Shizuku.addBinderDeadListener(internalBinderDeadListener)
            isRegistered = true
            recordDiagnostic(
                level = DiagnosticLevel.INFO,
                stage = "Manager Registration",
                message = "Registered Shizuku permission and binder lifecycle listeners."
            )
            Log.i(TAG, "Successfully registered Shizuku.OnRequestPermissionResultListener")
            checkPermission()
        } catch (e: Throwable) {
            recordDiagnostic(
                level = DiagnosticLevel.ERROR,
                stage = "Registration Error",
                message = "Failed to register Shizuku RequestPermissionResultListener: ${e.message}",
                technicalDetails = e.stackTraceToString(),
                suggestedFix = "Verify that the Shizuku provider and API dependencies are properly integrated."
            )
            Log.e(TAG, "Failed to register Shizuku RequestPermissionResultListener", e)
        }
    }

    /**
     * Unregisters the listener.
     */
    fun unregister() {
        if (!isRegistered) return
        try {
            Shizuku.removeRequestPermissionResultListener(this)
            Shizuku.removeBinderReceivedListener(internalBinderReceivedListener)
            Shizuku.removeBinderDeadListener(internalBinderDeadListener)
            isRegistered = false
            recordDiagnostic(
                level = DiagnosticLevel.INFO,
                stage = "Manager Cleanup",
                message = "Unregistered Shizuku listeners cleanly."
            )
            Log.i(TAG, "Unregistered Shizuku RequestPermissionResultListener")
        } catch (e: Throwable) {
            Log.w(TAG, "Error removing Shizuku RequestPermissionResultListener", e)
        }
    }

    /**
     * Direct callback invoked by the Shizuku framework when the user interacts
     * with the system authorization dialog.
     */
    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        val granted = (grantResult == PackageManager.PERMISSION_GRANTED)
        Log.i(TAG, "Shizuku permission result received: requestCode=$requestCode, granted=$granted")

        _isGranted.value = granted
        _permissionStatus.value = if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED

        recordDiagnostic(
            level = if (granted) DiagnosticLevel.SUCCESS else DiagnosticLevel.WARNING,
            stage = "Permission Result",
            message = if (granted) "Shizuku authorization granted by user!" else "Shizuku authorization denied by user.",
            technicalDetails = "requestCode=$requestCode, grantResult=$grantResult",
            suggestedFix = if (!granted) "Open Shizuku app -> 'Authorized Applications' -> enable Pixel Routines." else null
        )

        // Notify listeners immediately
        for (listener in changeListeners) {
            try {
                listener(granted)
            } catch (e: Throwable) {
                Log.e(TAG, "Error notifying permission change listener", e)
            }
        }

        // Emit coroutine event for observers
        scope.launch {
            _permissionEvents.emit(PermissionResultEvent(requestCode, granted))
        }

        // Synchronize with ShizukuServiceHelper state
        ShizukuServiceHelper.checkBindingAndPermission()
    }

    /**
     * Checks current permission status directly against Shizuku service.
     */
    fun checkPermission(): Boolean {
        return try {
            val isPing = Shizuku.pingBinder()
            if (!isPing) {
                _isGranted.value = false
                _permissionStatus.value = PermissionStatus.UNKNOWN
                return false
            }

            val hasPerm = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            _isGranted.value = hasPerm
            _permissionStatus.value = if (hasPerm) PermissionStatus.GRANTED else PermissionStatus.DENIED
            hasPerm
        } catch (e: Throwable) {
            recordDiagnostic(
                level = DiagnosticLevel.ERROR,
                stage = "Check Permission",
                message = "Error invoking Shizuku.checkSelfPermission(): ${e.message}",
                technicalDetails = e.stackTraceToString()
            )
            Log.w(TAG, "Could not check Shizuku permission", e)
            _isGranted.value = false
            _permissionStatus.value = PermissionStatus.UNKNOWN
            false
        }
    }

    /**
     * Dispatches the Shizuku permission prompt to the user.
     *
     * @return true if the prompt was successfully dispatched or permission was already granted.
     */
    fun requestPermission(requestCode: Int = DEFAULT_REQUEST_CODE): Boolean {
        return try {
            if (!Shizuku.pingBinder()) {
                recordDiagnostic(
                    level = DiagnosticLevel.ERROR,
                    stage = "Request Permission",
                    message = "Cannot request permission: Shizuku binder is offline / unreachable.",
                    suggestedFix = "Start Shizuku via Wireless Debugging first, then retry."
                )
                Log.w(TAG, "Cannot request permission: Shizuku binder is offline")
                return false
            }

            if (checkPermission()) {
                recordDiagnostic(
                    level = DiagnosticLevel.INFO,
                    stage = "Request Permission",
                    message = "Permission is already granted. Skipping prompt dispatch."
                )
                Log.i(TAG, "Permission already granted")
                return true
            }

            recordDiagnostic(
                level = DiagnosticLevel.INFO,
                stage = "Request Permission",
                message = "Dispatching Shizuku.requestPermission(requestCode=$requestCode)",
                technicalDetails = "Awaiting user confirmation in system dialog."
            )
            Log.i(TAG, "Requesting Shizuku permission with requestCode=$requestCode")
            Shizuku.requestPermission(requestCode)
            true
        } catch (e: Throwable) {
            recordDiagnostic(
                level = DiagnosticLevel.ERROR,
                stage = "Request Permission Error",
                message = "Exception dispatching Shizuku permission request: ${e.message}",
                technicalDetails = e.stackTraceToString(),
                suggestedFix = "Ensure Shizuku app is running in the background."
            )
            Log.e(TAG, "Exception while requesting Shizuku permission", e)
            false
        }
    }

    /**
     * Checks if Shizuku recommends showing a permission explanation rationale.
     */
    fun shouldShowRationale(): Boolean {
        return try {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                Shizuku.shouldShowRequestPermissionRationale()
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Attaches lifecycle callbacks to the application to automatically reconnect
     * Shizuku when the app is resumed after being in the background.
     */
    fun attachApplication(application: Application) {
        if (isAppLifecycleAttached) return
        isAppLifecycleAttached = true
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                Log.d(TAG, "Activity resumed: ${activity.javaClass.simpleName}, verifying Shizuku connection")
                onAppResumed()
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /**
     * Reconnects and re-binds to the Shizuku IPC binder service.
     * Specifically checks [Shizuku.pingBinder()] when connection state is lost,
     * resets listeners, re-attaches sticky binders, and checks permissions.
     *
     * @param force Force a re-binding pass even if currently thought to be active.
     * @return true if binder is responding and active, false otherwise.
     */
    fun reconnectShizuku(force: Boolean = false): Boolean {
        _lastReconnectAttempt.value = System.currentTimeMillis()
        _isReconnecting.value = true

        try {
            recordDiagnostic(
                level = DiagnosticLevel.INFO,
                stage = "Reconnect Shizuku",
                message = "Attempting to reconnect and re-bind Shizuku service (force=$force)...",
                technicalDetails = "Checking Shizuku.pingBinder() and resetting IPC hooks."
            )

            // Step 1: Initial binder ping check
            var isBinderAlive = false
            try {
                isBinderAlive = Shizuku.pingBinder()
            } catch (t: Throwable) {
                recordDiagnostic(
                    level = DiagnosticLevel.WARNING,
                    stage = "Reconnect Ping",
                    message = "Initial ping failed: ${t.javaClass.simpleName} - ${t.message}",
                    technicalDetails = t.stackTraceToString()
                )
            }

            // Step 2: If connection was lost or force requested, re-bind listeners
            if (!isBinderAlive || force) {
                try {
                    Shizuku.removeRequestPermissionResultListener(this)
                    Shizuku.removeBinderReceivedListener(internalBinderReceivedListener)
                    Shizuku.removeBinderDeadListener(internalBinderDeadListener)
                } catch (t: Throwable) {
                    // Safe cleanup
                }

                try {
                    Shizuku.addRequestPermissionResultListener(this)
                    Shizuku.addBinderReceivedListenerSticky(internalBinderReceivedListener)
                    Shizuku.addBinderDeadListener(internalBinderDeadListener)
                    isRegistered = true
                } catch (t: Throwable) {
                    recordDiagnostic(
                        level = DiagnosticLevel.ERROR,
                        stage = "Re-binding Listeners",
                        message = "Exception re-registering Shizuku listeners: ${t.message}",
                        technicalDetails = t.stackTraceToString()
                    )
                }

                // Step 3: Re-ping after re-binding
                isBinderAlive = try {
                    Shizuku.pingBinder()
                } catch (t: Throwable) {
                    false
                }
            }

            // Step 4: Handle outcomes & sync status
            if (isBinderAlive) {
                recordDiagnostic(
                    level = DiagnosticLevel.SUCCESS,
                    stage = "Reconnect Shizuku",
                    message = "Successfully re-bound and verified Shizuku binder!",
                    technicalDetails = "Shizuku.pingBinder() = true"
                )
                checkPermission()
                ShizukuServiceHelper.checkBindingAndPermission()
                ShizukuManager.checkStatus()
                return true
            } else {
                recordDiagnostic(
                    level = DiagnosticLevel.WARNING,
                    stage = "Reconnect Shizuku",
                    message = "Shizuku binder is currently offline or unreachable.",
                    technicalDetails = "Shizuku.pingBinder() returned false after re-binding attempt.",
                    suggestedFix = "Ensure Shizuku app is running. Open Shizuku -> Start via Wireless Debugging."
                )
                _isGranted.value = false
                _permissionStatus.value = PermissionStatus.UNKNOWN
                ShizukuManager.checkStatus()
                return false
            }
        } catch (e: Throwable) {
            recordDiagnostic(
                level = DiagnosticLevel.ERROR,
                stage = "Reconnect Error",
                message = "Unexpected failure in reconnectShizuku: ${e.message}",
                technicalDetails = e.stackTraceToString()
            )
            return false
        } finally {
            _isReconnecting.value = false
        }
    }

    /**
     * Alias for [reconnectShizuku].
     */
    fun reconnect(force: Boolean = false): Boolean = reconnectShizuku(force)

    /**
     * Specifically handles the case where the app is resumed after being backgrounded.
     * When returning from background (e.g. user went to Shizuku app to start wireless debugging,
     * or connected to Wi-Fi, or received a call), this validates `Shizuku.pingBinder()`
     * and attempts to re-bind the service if the connection was lost.
     */
    fun onAppResumed() {
        val pingSuccess = try { Shizuku.pingBinder() } catch (t: Throwable) { false }
        val currentlyGranted = _isGranted.value

        recordDiagnostic(
            level = DiagnosticLevel.INFO,
            stage = "App Resumed",
            message = "App resumed from background. Validating connection state (ping=$pingSuccess, granted=$currentlyGranted)..."
        )

        if (!pingSuccess || !currentlyGranted) {
            // Connection was lost or unverified: attempt re-binding
            reconnectShizuku(force = true)
        } else {
            // Connection is active: refresh permission and sync state
            checkPermission()
            ShizukuServiceHelper.checkBindingAndPermission()
            ShizukuManager.checkStatus()
        }
    }

    /**
     * Records a diagnostic message into memory and updates [diagnosticLogs].
     */
    fun recordDiagnostic(
        level: DiagnosticLevel,
        stage: String,
        message: String,
        technicalDetails: String? = null,
        suggestedFix: String? = null
    ) {
        val entry = ShizukuDiagnosticEntry(
            level = level,
            stage = stage,
            message = message,
            technicalDetails = technicalDetails,
            suggestedFix = suggestedFix
        )
        recordDiagnostic(entry)
    }

    /**
     * Records an existing diagnostic entry.
     */
    fun recordDiagnostic(entry: ShizukuDiagnosticEntry) {
        rawDiagnostics.add(0, entry) // Newest first
        while (rawDiagnostics.size > MAX_DIAGNOSTIC_LOGS) {
            rawDiagnostics.removeAt(rawDiagnostics.size - 1)
        }
        _diagnosticLogs.value = rawDiagnostics.toList()
    }

    /**
     * Clears diagnostic history.
     */
    fun clearDiagnostics() {
        rawDiagnostics.clear()
        _diagnosticLogs.value = emptyList()
    }

    /**
     * Runs a comprehensive end-to-end diagnosis of Shizuku components:
     * - Shizuku Manager application installation and package status
     * - ShizukuProvider declaration in AndroidManifest.xml
     * - Runtime permission declaration (`moe.shizuku.manager.permission.API_V23`)
     * - Direct binder ping and IPC availability
     * - Android OS Wireless Debugging developer setting state
     * - Self-permission check
     */
    fun runFullDiagnostics(context: Context): List<ShizukuDiagnosticEntry> {
        val results = mutableListOf<ShizukuDiagnosticEntry>()

        fun addResult(
            level: DiagnosticLevel,
            stage: String,
            message: String,
            details: String? = null,
            suggestedFix: String? = null
        ) {
            val entry = ShizukuDiagnosticEntry(
                level = level,
                stage = stage,
                message = message,
                technicalDetails = details,
                suggestedFix = suggestedFix
            )
            results.add(entry)
            recordDiagnostic(entry)
        }

        // 1. Shizuku Manager App Package Check
        val pm = context.packageManager
        try {
            val pkgInfo = pm.getPackageInfo("moe.shizuku.privileged.api", 0)
            val isEnabled = pkgInfo.applicationInfo?.enabled == true
            if (isEnabled) {
                addResult(
                    DiagnosticLevel.SUCCESS,
                    "Package Discovery",
                    "Shizuku Manager app is installed (version ${pkgInfo.versionName}, code ${pkgInfo.versionCode}).",
                    "Package: moe.shizuku.privileged.api, enabled: true"
                )
            } else {
                addResult(
                    DiagnosticLevel.ERROR,
                    "Package Discovery",
                    "Shizuku Manager app is installed but currently DISABLED in system settings.",
                    "Package: moe.shizuku.privileged.api",
                    "Enable Shizuku in Android App Settings."
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            addResult(
                DiagnosticLevel.ERROR,
                "Package Discovery",
                "Shizuku Manager app (moe.shizuku.privileged.api) is NOT installed on this device.",
                "PackageManager.NameNotFoundException",
                "Install Shizuku from https://shizuku.rikka.app or Google Play Store."
            )
        }

        // 2. Client ShizukuProvider Configuration Check
        try {
            val authority = "${context.packageName}.shizuku"
            val providerInfo = pm.resolveContentProvider(authority, 0)
            if (providerInfo != null) {
                addResult(
                    DiagnosticLevel.SUCCESS,
                    "Provider Bridge",
                    "Client ShizukuProvider is correctly configured for authority '$authority'.",
                    "Provider Class: ${providerInfo.name}, exported=${providerInfo.exported}"
                )
            } else {
                addResult(
                    DiagnosticLevel.ERROR,
                    "Provider Bridge",
                    "ShizukuProvider NOT found in AndroidManifest.xml for authority '$authority'.",
                    "Shizuku cannot transfer its binder to this client app without this provider.",
                    "Ensure rikka.shizuku.ShizukuProvider is declared in AndroidManifest.xml."
                )
            }
        } catch (e: Throwable) {
            addResult(
                DiagnosticLevel.WARNING,
                "Provider Bridge",
                "Failed to inspect ShizukuProvider: ${e.message}",
                e.stackTraceToString()
            )
        }

        // 3. Android Permission Manifest Check
        try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            }
            val hasPermDeclared = pkgInfo.requestedPermissions?.contains("moe.shizuku.manager.permission.API_V23") == true
            if (hasPermDeclared) {
                addResult(
                    DiagnosticLevel.SUCCESS,
                    "Manifest Permissions",
                    "moe.shizuku.manager.permission.API_V23 is present in AndroidManifest.xml."
                )
            } else {
                addResult(
                    DiagnosticLevel.ERROR,
                    "Manifest Permissions",
                    "moe.shizuku.manager.permission.API_V23 is MISSING from AndroidManifest.xml.",
                    null,
                    "Declare <uses-permission android:name=\"moe.shizuku.manager.permission.API_V23\" /> in AndroidManifest.xml."
                )
            }
        } catch (e: Throwable) {
            addResult(
                DiagnosticLevel.WARNING,
                "Manifest Permissions",
                "Could not inspect manifest permissions: ${e.message}"
            )
        }

        // 4. Binder Ping & IPC Check
        try {
            val isPingAlive = Shizuku.pingBinder()
            if (isPingAlive) {
                val version = try { Shizuku.getVersion() } catch (t: Throwable) { -1 }
                val uid = try { Shizuku.getUid() } catch (t: Throwable) { -1 }
                val identity = when (uid) {
                    0 -> "Root (UID 0)"
                    2000 -> "ADB Shell (UID 2000)"
                    else -> "UID $uid"
                }
                addResult(
                    DiagnosticLevel.SUCCESS,
                    "Binder Ping",
                    "Shizuku IPC binder is ALIVE and responding (API v$version, Identity: $identity).",
                    "Shizuku.pingBinder() = true."
                )
            } else {
                addResult(
                    DiagnosticLevel.ERROR,
                    "Binder Ping",
                    "Shizuku IPC binder is OFFLINE (pingBinder() returned false).",
                    "Possible causes:\n" +
                            "• Wireless Debugging stopped or disconnected.\n" +
                            "• Wi-Fi network disconnected or changed IP.\n" +
                            "• Device rebooted without re-starting Shizuku.\n" +
                            "• Shizuku service was killed by OS background limits.",
                    "1. Open Shizuku app.\n" +
                            "2. Check if Shizuku shows 'Running' or 'Not running'.\n" +
                            "3. If not running, tap 'Start via Wireless Debugging'.\n" +
                            "4. Ensure Wireless Debugging is enabled in Developer Options."
                )
            }
        } catch (e: Throwable) {
            addResult(
                DiagnosticLevel.ERROR,
                "Binder Ping Exception",
                "Exception pinging Shizuku binder: ${e.javaClass.simpleName} - ${e.message}",
                e.stackTraceToString(),
                "Restart the Shizuku daemon via Wireless Debugging."
            )
        }

        // 5. System Wireless Debugging Settings Check
        try {
            val adbWifiEnabled = Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", -1)
            val adbEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, -1)
            if (adbWifiEnabled == 1) {
                addResult(
                    DiagnosticLevel.SUCCESS,
                    "Wireless Debugging Setting",
                    "Android Developer Settings report Wireless Debugging is ON (adb_wifi_enabled=1).",
                    "ADB Enabled: $adbEnabled"
                )
            } else if (adbWifiEnabled == 0) {
                addResult(
                    DiagnosticLevel.WARNING,
                    "Wireless Debugging Setting",
                    "Android Developer Settings report Wireless Debugging is OFF (adb_wifi_enabled=0).",
                    "Android automatically turns Wireless Debugging OFF when disconnecting from Wi-Fi.",
                    "Open Developer Options -> Turn ON 'Wireless Debugging' toggle."
                )
            } else {
                addResult(
                    DiagnosticLevel.INFO,
                    "Wireless Debugging Setting",
                    "Developer settings status: adb=$adbEnabled, adb_wifi=$adbWifiEnabled.",
                    "Ensure Wireless Debugging is enabled on your current Wi-Fi network."
                )
            }
        } catch (e: Throwable) {
            addResult(
                DiagnosticLevel.INFO,
                "Wireless Debugging Setting",
                "Could not query Global.adb_wifi_enabled (${e.javaClass.simpleName})."
            )
        }

        // 6. Permission Authorization Check
        try {
            if (Shizuku.pingBinder()) {
                val hasPerm = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    addResult(
                        DiagnosticLevel.SUCCESS,
                        "Authorization Check",
                        "Pixel Routines is fully AUTHORIZED by Shizuku. Shell commands can execute directly."
                    )
                } else {
                    addResult(
                        DiagnosticLevel.WARNING,
                        "Authorization Check",
                        "Shizuku service is running, but Pixel Routines does not have permission granted yet.",
                        "Shizuku.checkSelfPermission() = PERMISSION_DENIED.",
                        "Tap 'Connect & Authorize' in the app or enable Pixel Routines inside Shizuku app -> Authorized Applications."
                    )
                }
            } else {
                addResult(
                    DiagnosticLevel.INFO,
                    "Authorization Check",
                    "Authorization check skipped because binder is offline."
                )
            }
        } catch (e: Throwable) {
            addResult(
                DiagnosticLevel.WARNING,
                "Authorization Check",
                "Error checking permission: ${e.message}",
                e.stackTraceToString()
            )
        }

        // 7. Network Interface & Wi-Fi Check (Explains why Wireless Debugging drops outside home)
        try {
            val isWifi = NetworkStatusMonitor.isWifiConnected.value
            val isCellular = NetworkStatusMonitor.isCellularConnected.value
            val netDesc = NetworkStatusMonitor.networkDescription.value
            if (isWifi) {
                addResult(
                    DiagnosticLevel.SUCCESS,
                    "Wi-Fi Network State",
                    "Device is connected to Wi-Fi ($netDesc). Wireless Debugging interface is active.",
                    "Local IP network interface is available for Shizuku IPC."
                )
            } else if (isCellular) {
                addResult(
                    DiagnosticLevel.WARNING,
                    "Wi-Fi Network State",
                    "Device is on Cellular Mobile Data ($netDesc) - NOT Wi-Fi!",
                    "Android OS automatically terminates the Wireless Debugging port when disconnected from Wi-Fi. This is why Shizuku stops working after leaving the house.",
                    "1. When returning home: Reconnect to Wi-Fi and tap Re-check.\n" +
                            "2. Away from home: Turn on 'Personal Hotspot' in Settings to create a local network for Wireless Debugging on the go.\n" +
                            "3. Keep Shizuku battery set to 'Unrestricted'."
                )
            } else {
                addResult(
                    DiagnosticLevel.WARNING,
                    "Wi-Fi Network State",
                    "No active network connection ($netDesc).",
                    "Wireless Debugging requires an active network interface.",
                    "Connect to Wi-Fi or enable Personal Hotspot."
                )
            }
        } catch (e: Throwable) {
            addResult(
                DiagnosticLevel.INFO,
                "Wi-Fi Network State",
                "Could not evaluate network state: ${e.message}"
            )
        }

        // 8. Battery Optimization Exemption Check
        try {
            val isIgnoringBattery = NetworkStatusMonitor.isAppBatteryOptimizationIgnored(context)
            if (isIgnoringBattery) {
                addResult(
                    DiagnosticLevel.SUCCESS,
                    "Battery Optimization",
                    "Pixel Routines is exempted from Android battery optimizations (Unrestricted background execution enabled)."
                )
            } else {
                addResult(
                    DiagnosticLevel.WARNING,
                    "Battery Optimization",
                    "Battery optimization is active for this app or Shizuku.",
                    "Android may kill Shizuku daemon or routine alarm timers when the screen is turned off or in your pocket.",
                    "Set Shizuku and Pixel Routines battery usage to 'Unrestricted' in App Info."
                )
            }
        } catch (e: Throwable) {
            addResult(
                DiagnosticLevel.INFO,
                "Battery Optimization",
                "Battery optimization check skipped: ${e.message}"
            )
        }

        return results
    }

    /**
     * Generates a plain-text markdown report suitable for clipboard sharing and debugging.
     */
    fun exportReport(context: Context): String {
        val entries = if (diagnosticLogs.value.isEmpty()) runFullDiagnostics(context) else diagnosticLogs.value
        val sb = StringBuilder()
        sb.append("=== Shizuku Connection Diagnostic Report ===\n")
        sb.append("Generated at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})\n")
        sb.append("App Package: ${context.packageName}\n\n")

        for (entry in entries) {
            val tag = when (entry.level) {
                DiagnosticLevel.SUCCESS -> "[OK]"
                DiagnosticLevel.INFO -> "[INFO]"
                DiagnosticLevel.WARNING -> "[WARN]"
                DiagnosticLevel.ERROR -> "[ERROR]"
            }
            sb.append("$tag [${entry.formattedTime}] [${entry.stage}]\n")
            sb.append("  Message: ${entry.message}\n")
            if (!entry.technicalDetails.isNullOrBlank()) {
                sb.append("  Details: ${entry.technicalDetails}\n")
            }
            if (!entry.suggestedFix.isNullOrBlank()) {
                sb.append("  Suggested Fix: ${entry.suggestedFix}\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * Register an observer callback that triggers whenever permission changes.
     */
    fun addOnPermissionChangeListener(listener: (Boolean) -> Unit) {
        changeListeners.add(listener)
    }

    /**
     * Remove an observer callback.
     */
    fun removeOnPermissionChangeListener(listener: (Boolean) -> Unit) {
        changeListeners.remove(listener)
    }
}
