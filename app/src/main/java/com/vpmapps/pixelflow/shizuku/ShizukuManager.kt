package com.vpmapps.pixelflow.shizuku

import kotlinx.coroutines.flow.StateFlow

/**
 * Singleton gateway that delegates core binder and process execution
 * to ShizukuServiceHelper, while exposing specialized system setting actions.
 */
object ShizukuManager {
    val helper: ShizukuServiceHelper = ShizukuServiceHelper
    val permissionManager: ShizukuPermissionManager = ShizukuPermissionManager

    val state: StateFlow<ShizukuState> = helper.state
    val version: StateFlow<Int> = helper.version
    val isBinderAlive: StateFlow<Boolean> = helper.isBinderAlive
    val isPermissionGranted: StateFlow<Boolean> = permissionManager.isGranted

    fun init() {
        helper.initialize()
        permissionManager.register()
    }

    fun checkStatus() {
        helper.checkBindingAndPermission()
        permissionManager.checkPermission()
    }

    fun checkPermission(): Boolean {
        return permissionManager.checkPermission()
    }

    fun isAuthorized(): Boolean {
        return helper.isPermissionGranted()
    }

    fun requestPermission(requestCode: Int = ShizukuPermissionManager.DEFAULT_REQUEST_CODE): Boolean {
        return permissionManager.requestPermission(requestCode)
    }

    suspend fun executeCommand(command: String): ShellResult {
        return helper.executeCommand(command)
    }

    // High-level system actions using Shizuku privileged shell
    suspend fun setAod(enabled: Boolean): ShellResult {
        val cmd = "settings put secure doze_always_on ${if (enabled) 1 else 0}"
        return executeCommand(cmd)
    }

    suspend fun setAutoRotate(enabled: Boolean): ShellResult {
        val cmd = "settings put system accelerometer_rotation ${if (enabled) 1 else 0}"
        return executeCommand(cmd)
    }

    suspend fun setDarkMode(enabled: Boolean): ShellResult {
        val cmd = if (enabled) "cmd uimode night yes || settings put secure ui_night_mode 2" else "cmd uimode night no || settings put secure ui_night_mode 1"
        return executeCommand(cmd)
    }

    suspend fun setWifi(enabled: Boolean): ShellResult {
        val cmd = if (enabled) {
            "cmd wifi set-wifi-enabled enabled || svc wifi enable || settings put global wifi_on 1"
        } else {
            "cmd wifi set-wifi-enabled disabled || svc wifi disable || settings put global wifi_on 0"
        }
        return executeCommand(cmd)
    }

    suspend fun setHotspot(enabled: Boolean): ShellResult {
        val cmd = if (enabled) {
            "cmd wifi start-softap || svc wifi startSoftAp || cmd connectivity start-tethering 0 || cmd tethering start-tethering 0"
        } else {
            "cmd wifi stop-softap || svc wifi stopSoftAp || cmd connectivity stop-tethering 0 || cmd tethering stop-tethering 0"
        }
        return executeCommand(cmd)
    }

    suspend fun setBluetooth(enabled: Boolean): ShellResult {
        val cmd = if (enabled) {
            "cmd bluetooth_manager enable || svc bluetooth enable || settings put global bluetooth_on 1"
        } else {
            "cmd bluetooth_manager disable || svc bluetooth disable || settings put global bluetooth_on 0"
        }
        return executeCommand(cmd)
    }

    suspend fun setBatterySaver(enabled: Boolean): ShellResult {
        val cmd = "cmd power set-mode ${if (enabled) 1 else 0} || settings put global low_power ${if (enabled) 1 else 0}"
        return executeCommand(cmd)
    }

    suspend fun setScreenTimeout(timeoutMs: Int): ShellResult {
        val cmd = "settings put system screen_off_timeout $timeoutMs"
        return executeCommand(cmd)
    }

    suspend fun grantNotificationPolicyAccess(targetPackageName: String = com.vpmapps.pixelflow.BuildConfig.APPLICATION_ID): ShellResult {
        val cmd = "appops set $targetPackageName ACCESS_NOTIFICATION_POLICY allow"
        return executeCommand(cmd)
    }

    suspend fun setSoundProfile(
        mode: String,
        volumePercent: Int = -1,
        targetPackageName: String = com.vpmapps.pixelflow.BuildConfig.APPLICATION_ID
    ): ShellResult {
        val upperMode = mode.uppercase()

        val commands = mutableListOf<String>()

        // 1. Ensure Do Not Disturb Access is granted to Pixel Routines via AppOps so AudioManager & system APIs work natively
        commands.add("appops set $targetPackageName ACCESS_NOTIFICATION_POLICY allow || true")

        // 2. Comprehensive Ringer Mode Command Chain
        when (upperMode) {
            "VIBRATE" -> {
                // Ensure DND / Zen mode is disengaged, set ringer to VIBRATE via AudioService and system settings
                commands.add("cmd notification set_dnd off || cmd notification set_dnd all || true")
                commands.add("cmd audio set-ringer-mode VIBRATE || cmd audio set-ringer-mode vibrate || true")
                commands.add("settings put global mode_ringer 1")
                commands.add("settings put global zen_mode 0")
                // Silence ring/notification streams so it won't produce audible ringtones, keeping haptic feedback
                commands.add(
                    "cmd media_session volume --stream 2 --set 0 || " +
                    "media volume --stream 2 --set 0 || true"
                )
            }
            "SILENT" -> {
                // Set ringer to SILENT and turn on DND
                commands.add("cmd audio set-ringer-mode SILENT || cmd audio set-ringer-mode silent || true")
                commands.add("cmd notification set_dnd priority || cmd notification set_dnd on || true")
                commands.add("settings put global mode_ringer 0")
                commands.add("settings put global zen_mode 2")
                commands.add(
                    "cmd media_session volume --stream 2 --set 0 || " +
                    "media volume --stream 2 --set 0 || true"
                )
            }
            else -> { // NORMAL / RING
                // Turn off DND and restore NORMAL ringer mode
                commands.add("cmd notification set_dnd off || cmd notification set_dnd all || true")
                commands.add("cmd audio set-ringer-mode NORMAL || cmd audio set-ringer-mode normal || true")
                commands.add("settings put global mode_ringer 2")
                commands.add("settings put global zen_mode 0")
            }
        }

        // 3. Media / Ring Stream Volume Control if specified
        if (volumePercent in 0..100) {
            commands.add(
                "cmd media_session volume --stream 3 --set $volumePercent || " +
                "media volume --stream 3 --set $volumePercent || true"
            )
            if (upperMode != "SILENT" && upperMode != "VIBRATE") {
                commands.add(
                    "cmd media_session volume --stream 2 --set $volumePercent || " +
                    "media volume --stream 2 --set $volumePercent || true"
                )
            }
        }

        return executeCommand(commands.joinToString(" ; "))
    }

    suspend fun queryAodStatus(): String {
        val res = executeCommand("settings get secure doze_always_on")
        return if (res.isSuccess) res.stdout else "unknown"
    }
}
