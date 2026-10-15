package com.vpmapps.pixelflow.service

import android.app.NotificationManager
import android.app.UiModeManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.vpmapps.pixelflow.data.local.AppDatabase
import com.vpmapps.pixelflow.data.model.ActionType
import com.vpmapps.pixelflow.data.model.CustomNotificationConfig
import com.vpmapps.pixelflow.data.model.RoutineActionItem
import com.vpmapps.pixelflow.data.model.RoutineEntity
import com.vpmapps.pixelflow.data.model.RoutineExecutionLog
import com.vpmapps.pixelflow.notification.RoutineNotificationManager
import com.vpmapps.pixelflow.shizuku.ShellResult
import com.vpmapps.pixelflow.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Tracks actively executing routine IDs in real-time across the application.
 */
object RoutineExecutionTracker {
    private val _runningRoutineIds = MutableStateFlow<Set<Long>>(emptySet())
    val runningRoutineIds: StateFlow<Set<Long>> = _runningRoutineIds.asStateFlow()

    fun markRunning(routineId: Long) {
        _runningRoutineIds.update { it + routineId }
    }

    fun markFinished(routineId: Long) {
        _runningRoutineIds.update { it - routineId }
    }

    fun isRunning(routineId: Long): Boolean = _runningRoutineIds.value.contains(routineId)

    private val locks = java.util.concurrent.ConcurrentHashMap<Long, Mutex>()

    /** One lock per routine so a widget tap, an alarm and a trigger can't run it at the same time. */
    fun lockFor(routineId: Long): Mutex = locks.getOrPut(routineId) { Mutex() }
}

class RoutineExecutionEngine(private val context: Context) {
    private val TAG = "RoutineExecutionEngine"
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)
    private val dao = database.routineDao()

    /**
     * Runs the routine's actions. If the same routine is already running (e.g. a widget tap while its
     * alarm fires), the second request is skipped instead of interleaving with the first, which
     * would capture a half-applied settings snapshot.
     */
    suspend fun executeRoutine(routine: RoutineEntity, triggerReason: String = "Manual Execution"): Boolean {
        val lock = RoutineExecutionTracker.lockFor(routine.id)
        if (!lock.tryLock()) {
            Log.i(TAG, "Routine ${routine.title} is already running; skipping duplicate trigger ($triggerReason)")
            return false
        }
        return try {
            executeRoutineLocked(routine, triggerReason)
        } finally {
            lock.unlock()
        }
    }

    private suspend fun executeRoutineLocked(routine: RoutineEntity, triggerReason: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Executing routine: ${routine.title} (Trigger: $triggerReason)")
        RoutineExecutionTracker.markRunning(routine.id)
        try {
            com.vpmapps.pixelflow.widget.WidgetUpdateHelper.updateAllWidgets(appContext)
        } catch (e: Throwable) {
            // Safe widget update
        }
        try {
            dao.updateExecutionStatus(routine.id, System.currentTimeMillis(), "RUNNING")
        } catch (e: Exception) {
            Log.w(TAG, "Failed updating execution status to RUNNING", e)
        }

        val actions = routine.parseActions()
        // Always ensure Shizuku binder is attached and verified before executing actions
        val shizukuReady = com.vpmapps.pixelflow.shizuku.ShizukuServiceHelper.ensureAuthorized(timeoutMs = 2500L)

        // Capture snapshot of current hardware/system settings before modifying them
        if (routine.restoreOnEnd && actions.isNotEmpty()) {
            try {
                val currentSnapshot = RoutineStateSnapshotManager.captureCurrentSettingsForActions(appContext, actions)
                RoutineStateSnapshotManager.saveSnapshotIfNotExists(appContext, routine.id, currentSnapshot, triggerReason)
            } catch (e: Exception) {
                Log.w(TAG, "Failed capturing state snapshot prior to executing routine", e)
            }
        }

        val logBuilder = StringBuilder()
        val commandsExecuted = mutableListOf<String>()
        val stdoutList = mutableListOf<String>()
        val errorList = mutableListOf<String>()
        var overallSuccess = true
        var shizukuUsed = false
        var lastExitCode = 0

        try {
            for (action in actions) {
                val result = executeAction(action)
                if (action.type.requiresShizuku) {
                    shizukuUsed = true
                }
                if (!result.isSuccess) {
                    overallSuccess = false
                }
                if (result.exitCode != 0) {
                    lastExitCode = result.exitCode
                }
                if (result.executedCommand.isNotBlank()) {
                    commandsExecuted.add(result.executedCommand)
                }
                if (result.stdout.isNotBlank()) {
                    stdoutList.add("[${action.type.name}] ${result.stdout}")
                }
                if (result.stderr.isNotBlank()) {
                    errorList.add("[${action.type.name}] Error: ${result.stderr}")
                }

                logBuilder.append("[${action.type.name}] -> ")
                if (result.stdout.isNotBlank()) {
                    logBuilder.append(result.stdout).append(" | ")
                }
                if (result.stderr.isNotBlank()) {
                    logBuilder.append("Error: ").append(result.stderr).append(" | ")
                }
                if (result.stdout.isBlank() && result.stderr.isBlank()) {
                    logBuilder.append(if (result.isSuccess) "Applied" else "Failed").append(" | ")
                }
            }
        } finally {
            RoutineExecutionTracker.markFinished(routine.id)
            try {
                com.vpmapps.pixelflow.widget.WidgetUpdateHelper.updateAllWidgets(appContext)
            } catch (e: Throwable) {
                // Safe widget update
            }
        }

        val logOutput = logBuilder.toString().trimEnd(' ', '|')
        val statusStr = if (overallSuccess) {
            "SUCCESS"
        } else {
            "FAILED"
        }

        // Update Routine record
        dao.updateExecutionStatus(routine.id, System.currentTimeMillis(), statusStr)

        // Insert comprehensive execution history log
        val log = RoutineExecutionLog(
            routineId = routine.id,
            routineTitle = routine.title,
            timestamp = System.currentTimeMillis(),
            isSuccess = overallSuccess,
            status = statusStr,
            executedCommands = commandsExecuted.joinToString("\n"),
            stdout = stdoutList.joinToString("\n"),
            errorMessage = if (errorList.isNotEmpty()) errorList.joinToString("\n") else null,
            exitCode = lastExitCode,
            triggerSummary = "$triggerReason (${routine.getTriggerSummary()})",
            actionsSummary = actions.joinToString(", ") { it.toSummary() },
            shizukuUsed = shizukuUsed,
            outputDetails = logOutput
        )
        dao.insertLog(log)

        if (overallSuccess) {
            com.vpmapps.pixelflow.review.ReviewPrompter.recordSuccessfulRun(appContext)
        }

        if (!overallSuccess && !shizukuReady && actions.any { it.type.requiresShizuku }) {
            RoutineNotificationManager.notifyShizukuUnavailable(appContext, routine.title)
        }

        // Post system notification detailing routine and actions taken
        try {
            RoutineNotificationManager.notifyRoutineExecuted(
                context = appContext,
                routine = routine,
                actions = actions,
                triggerReason = triggerReason,
                isSuccess = overallSuccess,
                status = statusStr
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error posting routine execution notification", e)
        }

        overallSuccess
    }

    /**
     * Restores the device settings to the exact state saved prior to routine execution.
     * Triggered when routine conditions end (e.g., Bluetooth disconnected, Wi-Fi lost,
     * time window finished, or manual stop).
     */
    suspend fun restoreRoutineSettings(routine: RoutineEntity, endReason: String = "Routine Ended"): Boolean {
        // Waits for a running execution of the same routine to finish so the snapshot is complete
        return RoutineExecutionTracker.lockFor(routine.id).withLock {
            restoreRoutineSettingsLocked(routine, endReason)
        }
    }

    private suspend fun restoreRoutineSettingsLocked(routine: RoutineEntity, endReason: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Restoring previous settings for routine: ${routine.title} ($endReason)")
        val snapshot = RoutineStateSnapshotManager.getSnapshot(appContext, routine.id)
        if (snapshot.isNullOrEmpty()) {
            Log.w(TAG, "No snapshot found to restore for routine ${routine.title} (ID: ${routine.id})")
            RoutineStateSnapshotManager.clearSnapshot(appContext, routine.id)
            dao.updateExecutionStatus(routine.id, System.currentTimeMillis(), "IDLE")
            return@withContext false
        }

        // Always ensure Shizuku binder is attached before executing restore actions
        com.vpmapps.pixelflow.shizuku.ShizukuServiceHelper.ensureAuthorized(timeoutMs = 2500L)
        val logBuilder = StringBuilder()
        val commandsExecuted = mutableListOf<String>()
        val stdoutList = mutableListOf<String>()
        val errorList = mutableListOf<String>()
        var overallSuccess = true
        var shizukuUsed = false
        var lastExitCode = 0

        RoutineExecutionTracker.markRunning(routine.id)
        try {
            for (action in snapshot) {
                val result = executeAction(action)
                if (action.type.requiresShizuku) {
                    shizukuUsed = true
                }
                if (!result.isSuccess) {
                    overallSuccess = false
                }
                if (result.exitCode != 0) {
                    lastExitCode = result.exitCode
                }
                if (result.executedCommand.isNotBlank()) {
                    commandsExecuted.add(result.executedCommand)
                }
                if (result.stdout.isNotBlank()) {
                    stdoutList.add("[Restore ${action.type.name}] ${result.stdout}")
                }
                if (result.stderr.isNotBlank()) {
                    errorList.add("[Restore ${action.type.name}] Error: ${result.stderr}")
                }
                logBuilder.append("[Restore ${action.type.name}] -> ")
                if (result.stdout.isNotBlank()) {
                    logBuilder.append(result.stdout).append(" | ")
                }
                if (result.stderr.isNotBlank()) {
                    logBuilder.append("Error: ").append(result.stderr).append(" | ")
                }
                if (result.stdout.isBlank() && result.stderr.isBlank()) {
                    logBuilder.append(if (result.isSuccess) "Restored" else "Failed").append(" | ")
                }
            }
        } finally {
            RoutineStateSnapshotManager.clearSnapshot(appContext, routine.id)
            RoutineExecutionTracker.markFinished(routine.id)
            try {
                com.vpmapps.pixelflow.widget.WidgetUpdateHelper.updateAllWidgets(appContext)
            } catch (e: Throwable) {
                // Safe widget update
            }
        }

        val logOutput = logBuilder.toString().trimEnd(' ', '|')
        val statusStr = if (overallSuccess) "RESTORED" else "RESTORE_FAILED"

        // Update Routine record back to IDLE
        dao.updateExecutionStatus(routine.id, System.currentTimeMillis(), "IDLE")

        // Record execution history log
        val log = RoutineExecutionLog(
            routineId = routine.id,
            routineTitle = routine.title,
            timestamp = System.currentTimeMillis(),
            isSuccess = overallSuccess,
            status = statusStr,
            executedCommands = commandsExecuted.joinToString("\n"),
            stdout = stdoutList.joinToString("\n"),
            errorMessage = if (errorList.isNotEmpty()) errorList.joinToString("\n") else null,
            exitCode = lastExitCode,
            triggerSummary = "$endReason (${routine.getTriggerSummary()})",
            actionsSummary = "Restored: ${snapshot.joinToString(", ") { it.toSummary() }}",
            shizukuUsed = shizukuUsed,
            outputDetails = logOutput
        )
        dao.insertLog(log)

        // Post restored notification
        try {
            RoutineNotificationManager.notifyRoutineRestored(
                context = appContext,
                routine = routine,
                restoredActions = snapshot,
                reason = endReason,
                isSuccess = overallSuccess
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error posting restore notification", e)
        }

        overallSuccess
    }

    suspend fun executeAction(action: RoutineActionItem): ShellResult = withContext(Dispatchers.IO) {
        when (action.type) {
            ActionType.AOD -> {
                ShizukuManager.setAod(action.enabledState)
            }
            ActionType.AUTO_ROTATE -> {
                try {
                    android.provider.Settings.System.putInt(
                        appContext.contentResolver,
                        android.provider.Settings.System.ACCELEROMETER_ROTATION,
                        if (action.enabledState) 1 else 0
                    )
                } catch (e: Throwable) {
                    // Requires WRITE_SETTINGS or privileged shell
                }
                ShizukuManager.setAutoRotate(action.enabledState)
            }
            ActionType.DARK_MODE -> {
                try {
                    val uiModeManager = appContext.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && uiModeManager != null) {
                        uiModeManager.setApplicationNightMode(
                            if (action.enabledState) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Native UiModeManager set failed: ${e.message}")
                }
                ShizukuManager.setDarkMode(action.enabledState)
            }
            ActionType.SOUND_PROFILE -> {
                applySoundProfile(action)
            }
            ActionType.WIFI -> {
                try {
                    val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                    @Suppress("DEPRECATION")
                    wifiManager?.isWifiEnabled = action.enabledState
                } catch (e: Throwable) {
                    // System API restriction
                }
                ShizukuManager.setWifi(action.enabledState)
            }
            ActionType.HOTSPOT -> {
                ShizukuManager.setHotspot(action.enabledState)
            }
            ActionType.BLUETOOTH -> {
                // BluetoothAdapter.enable()/disable() are no-ops for apps on Android 13+, so the
                // native call is only attempted on older versions; Shizuku does the real work.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && hasBluetoothConnectPermission()) {
                    try {
                        @Suppress("DEPRECATION")
                        val btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                        @Suppress("DEPRECATION", "MissingPermission")
                        if (action.enabledState) btAdapter?.enable() else btAdapter?.disable()
                    } catch (e: Throwable) {
                        // Privileged fallback
                    }
                }
                ShizukuManager.setBluetooth(action.enabledState)
            }
            ActionType.BATTERY_SAVER -> {
                ShizukuManager.setBatterySaver(action.enabledState)
            }
            ActionType.SCREEN_TIMEOUT -> {
                val timeoutMs = if (action.intValue > 0) action.intValue else 60000
                try {
                    android.provider.Settings.System.putInt(
                        appContext.contentResolver,
                        android.provider.Settings.System.SCREEN_OFF_TIMEOUT,
                        timeoutMs
                    )
                } catch (e: Throwable) {
                    // Privileged fallback
                }
                ShizukuManager.setScreenTimeout(timeoutMs)
            }
            ActionType.CUSTOM_NOTIFICATION -> {
                executeCustomNotification(action)
            }
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun executeCustomNotification(action: RoutineActionItem): ShellResult {
        return try {
            val cfg = CustomNotificationConfig.fromJsonString(action.stringValue)
            if (cfg.scheduleType == "IMMEDIATE" || cfg.scheduleType.isBlank()) {
                RoutineNotificationManager.postCustomNotification(
                    context = appContext,
                    title = cfg.title,
                    message = cfg.message,
                    iconName = cfg.iconName
                )
                ShellResult(
                    exitCode = 0,
                    stdout = "Custom notification '${cfg.title}' posted immediately",
                    stderr = "",
                    executedCommand = "post_custom_notification",
                    isSuccess = true
                )
            } else {
                // Timed notifications are scheduled when the routine is saved or enabled
                // (RoutineAlarmManager.scheduleRoutineCustomNotifications); scheduling again here
                // would post the same reminder twice.
                ShellResult(
                    exitCode = 0,
                    stdout = "Custom notification '${cfg.title}' is scheduled for ${cfg.time} (${cfg.scheduleType})",
                    stderr = "",
                    executedCommand = "schedule_custom_notification",
                    isSuccess = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed executing custom notification action", e)
            ShellResult(
                exitCode = 1,
                stdout = "",
                stderr = "Error: ${e.message}",
                executedCommand = "post_custom_notification",
                isSuccess = false
            )
        }
    }

    private suspend fun applySoundProfile(action: RoutineActionItem): ShellResult {
        val mode = action.stringValue.uppercase()
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val targetPkg = appContext.packageName

        var audioManagerSucceeded = false
        var exceptionMessage: String? = null

        // 1. If Shizuku is authorized or can connect, proactively grant ACCESS_NOTIFICATION_POLICY to the app via AppOps
        // and execute the privileged audio service commands
        var shizukuResult: ShellResult? = null
        if (ShizukuManager.isAuthorized() || com.vpmapps.pixelflow.shizuku.ShizukuServiceHelper.ensureAuthorized(timeoutMs = 2000L)) {
            try {
                ShizukuManager.executeCommand("appops set $targetPkg ACCESS_NOTIFICATION_POLICY allow")
            } catch (e: Throwable) {
                Log.w(TAG, "Failed pre-granting ACCESS_NOTIFICATION_POLICY via Shizuku", e)
            }
            shizukuResult = ShizukuManager.setSoundProfile(action.stringValue, action.intValue, targetPkg)
        }

        // 2. Apply via standard Android NotificationManager and AudioManager APIs
        try {
            val hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted == true
            when (mode) {
                "VIBRATE" -> {
                    if (hasDndAccess) {
                        try {
                            // Disengage any active DND filter so the ringer switch takes full effect
                            notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        } catch (e: Throwable) {
                            Log.w(TAG, "Error setting interruption filter for vibrate: ${e.message}")
                        }
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    audioManagerSucceeded = true
                }
                "SILENT" -> {
                    if (hasDndAccess) {
                        try {
                            notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                        } catch (e: Throwable) {
                            Log.w(TAG, "Error setting interruption filter for silent: ${e.message}")
                        }
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    audioManagerSucceeded = true
                }
                "NORMAL", "RING" -> {
                    if (hasDndAccess) {
                        try {
                            notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        } catch (e: Throwable) {
                            Log.w(TAG, "Error setting interruption filter for normal: ${e.message}")
                        }
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    audioManagerSucceeded = true
                }
            }

            // Adjust stream volume if specified
            if (action.intValue in 0..100) {
                val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetMusic = (maxMusic * (action.intValue / 100f)).toInt()
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetMusic, 0)
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot set media volume: ${e.message}")
                }

                if (mode != "SILENT" && mode != "VIBRATE") {
                    val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                    val targetRing = (maxRing * (action.intValue / 100f)).toInt()
                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, targetRing, 0)
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot set ring volume: ${e.message}")
                    }
                }
            }
        } catch (e: SecurityException) {
            exceptionMessage = "Permission denied: Requires Do Not Disturb (Notification Policy) access or Shizuku (${e.message})"
            Log.w(TAG, "SecurityException while changing ringer mode: ${e.message}")
        } catch (e: Exception) {
            exceptionMessage = "AudioManager error: ${e.message}"
            Log.w(TAG, "Exception while changing ringer mode: ${e.message}")
        }

        // 3. Hardware state verification: Check if device actually adopted the requested ringer mode
        val actualMode = audioManager.ringerMode
        val isVerified = when (mode) {
            "VIBRATE" -> actualMode == AudioManager.RINGER_MODE_VIBRATE
            "SILENT" -> actualMode == AudioManager.RINGER_MODE_SILENT
            "NORMAL", "RING" -> actualMode == AudioManager.RINGER_MODE_NORMAL
            else -> true
        }

        val modeName = when (actualMode) {
            AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
            AudioManager.RINGER_MODE_SILENT -> "SILENT"
            AudioManager.RINGER_MODE_NORMAL -> "SOUND/NORMAL"
            else -> "MODE_$actualMode"
        }

        return if (isVerified) {
            ShellResult(
                exitCode = 0,
                stdout = "Sound profile successfully applied: $mode (Confirmed hardware state: $modeName)",
                stderr = "",
                executedCommand = "cmd audio set-ringer-mode $mode / AudioManager.ringerMode=$mode",
                isSuccess = true
            )
        } else if (shizukuResult != null && shizukuResult.isSuccess) {
            // Re-check after Shizuku commands had a moment to settle
            val recheckMode = audioManager.ringerMode
            val recheckVerified = when (mode) {
                "VIBRATE" -> recheckMode == AudioManager.RINGER_MODE_VIBRATE
                "SILENT" -> recheckMode == AudioManager.RINGER_MODE_SILENT
                "NORMAL", "RING" -> recheckMode == AudioManager.RINGER_MODE_NORMAL
                else -> true
            }
            if (recheckVerified) {
                ShellResult(
                    exitCode = 0,
                    stdout = "Sound profile applied via Shizuku: $mode",
                    stderr = "",
                    executedCommand = "cmd audio set-ringer-mode $mode",
                    isSuccess = true
                )
            } else {
                ShellResult(
                    exitCode = 1,
                    stdout = "",
                    stderr = "Sound mode could not be changed to $mode (Current state: $modeName). Please grant 'Do Not Disturb' (Notification Policy) permission in system settings.",
                    executedCommand = "cmd audio set-ringer-mode $mode",
                    isSuccess = false
                )
            }
        } else {
            val failureReason = exceptionMessage ?: "Do Not Disturb (Notification Policy) access is not granted to PixelFlow and Shizuku is offline."
            ShellResult(
                exitCode = 1,
                stdout = "",
                stderr = "Sound mode could not be changed to $mode: $failureReason",
                executedCommand = "AudioManager.setRingerMode($mode)",
                isSuccess = false
            )
        }
    }
}
