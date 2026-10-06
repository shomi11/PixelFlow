package com.example.service

import android.app.NotificationManager
import android.app.UiModeManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.ActionType
import com.example.data.model.RoutineActionItem
import com.example.data.model.RoutineEntity
import com.example.data.model.RoutineExecutionLog
import com.example.notification.RoutineNotificationManager
import com.example.shizuku.ShellResult
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoutineExecutionEngine(private val context: Context) {
    private val TAG = "RoutineExecutionEngine"
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.routineDao()

    suspend fun executeRoutine(routine: RoutineEntity, triggerReason: String = "Manual Execution"): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Executing routine: ${routine.title} (Trigger: $triggerReason)")
        val actions = routine.parseActions()
        val logBuilder = StringBuilder()
        val commandsExecuted = mutableListOf<String>()
        val stdoutList = mutableListOf<String>()
        val errorList = mutableListOf<String>()
        var overallSuccess = true
        var shizukuUsed = false
        var lastExitCode = 0

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

        val logOutput = logBuilder.toString().trimEnd(' ', '|')
        val statusStr = if (overallSuccess) {
            if (ShizukuManager.isAuthorized()) "SUCCESS" else "SIMULATED"
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

        // Post system notification detailing routine and actions taken
        try {
            RoutineNotificationManager.notifyRoutineExecuted(
                context = context,
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

    suspend fun executeAction(action: RoutineActionItem): ShellResult = withContext(Dispatchers.IO) {
        when (action.type) {
            ActionType.AOD -> {
                ShizukuManager.setAod(action.enabledState)
            }
            ActionType.AUTO_ROTATE -> {
                ShizukuManager.setAutoRotate(action.enabledState)
            }
            ActionType.DARK_MODE -> {
                // If UiModeManager is accessible, we can also set it natively in addition to ADB command
                try {
                    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
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
                ShizukuManager.setWifi(action.enabledState)
            }
            ActionType.BLUETOOTH -> {
                ShizukuManager.setBluetooth(action.enabledState)
            }
            ActionType.BATTERY_SAVER -> {
                ShizukuManager.setBatterySaver(action.enabledState)
            }
            ActionType.SCREEN_TIMEOUT -> {
                val timeoutMs = if (action.intValue > 0) action.intValue else 60000
                ShizukuManager.setScreenTimeout(timeoutMs)
            }
        }
    }

    private suspend fun applySoundProfile(action: RoutineActionItem): ShellResult {
        var audioManagerSucceeded = false
        val mode = action.stringValue.uppercase()

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (mode) {
                "SILENT" -> {
                    try {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                        audioManagerSucceeded = true
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot change ringer mode to silent directly: ${e.message}")
                    }
                }
                "VIBRATE" -> {
                    try {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        audioManagerSucceeded = true
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot change ringer mode to vibrate: ${e.message}")
                    }
                }
                "NORMAL", "RING" -> {
                    try {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        audioManagerSucceeded = true
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot change ringer mode to normal: ${e.message}")
                    }
                }
            }

            // Adjust volume if specified
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
        } catch (e: Exception) {
            Log.w(TAG, "AudioManager execution error: ${e.message}")
        }

        // If Shizuku is authorized, execute privileged audio commands to guarantee DND bypass
        return if (ShizukuManager.isAuthorized()) {
            val shizukuResult = ShizukuManager.setSoundProfile(action.stringValue, action.intValue)
            if (shizukuResult.isSuccess || audioManagerSucceeded) {
                ShellResult(
                    exitCode = 0,
                    stdout = "Audio configured: $mode (Vol: ${if (action.intValue >= 0) "${action.intValue}%" else "unchanged"})",
                    stderr = "",
                    executedCommand = "cmd audio set-ringer-mode / media volume",
                    isSuccess = true
                )
            } else {
                shizukuResult
            }
        } else {
            ShellResult(
                exitCode = if (audioManagerSucceeded) 0 else 1,
                stdout = "Audio set via AudioManager: $mode",
                stderr = if (audioManagerSucceeded) "" else "Requires Notification Policy or Shizuku permission",
                executedCommand = "AudioManager.setRingerMode / setStreamVolume",
                isSuccess = audioManagerSucceeded
            )
        }
    }
}
