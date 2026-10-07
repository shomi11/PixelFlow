package com.example.shizuku

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
        val cmd = "cmd uimode night ${if (enabled) "yes" else "no"}"
        return executeCommand(cmd)
    }

    suspend fun setWifi(enabled: Boolean): ShellResult {
        val cmd = "svc wifi ${if (enabled) "enable" else "disable"}"
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
        val cmd = "cmd bluetooth_manager ${if (enabled) "enable" else "disable"} || svc bluetooth ${if (enabled) "enable" else "disable"}"
        return executeCommand(cmd)
    }

    suspend fun setBatterySaver(enabled: Boolean): ShellResult {
        val cmd = "cmd power set-mode ${if (enabled) 1 else 0}"
        return executeCommand(cmd)
    }

    suspend fun setScreenTimeout(timeoutMs: Int): ShellResult {
        val cmd = "settings put system screen_off_timeout $timeoutMs"
        return executeCommand(cmd)
    }

    suspend fun setSoundProfile(mode: String, volumePercent: Int = -1): ShellResult {
        val ringerCode = when (mode.uppercase()) {
            "SILENT" -> 0
            "VIBRATE" -> 1
            "NORMAL", "RING" -> 2
            else -> 2
        }

        val commands = mutableListOf<String>()
        commands.add("cmd audio set-ringer-mode $ringerCode || settings put global mode_ringer $ringerCode")

        if (ringerCode == 0) {
            commands.add("settings put global zen_mode 2")
        } else {
            commands.add("settings put global zen_mode 0")
        }

        if (volumePercent in 0..100) {
            commands.add("cmd media_session volume --stream 3 --set $volumePercent || media volume --stream 3 --set $volumePercent")
            if (ringerCode == 2) {
                commands.add("cmd media_session volume --stream 2 --set $volumePercent || media volume --stream 2 --set $volumePercent")
            }
        }

        return executeCommand(commands.joinToString(" && "))
    }

    suspend fun queryAodStatus(): String {
        val res = executeCommand("settings get secure doze_always_on")
        return if (res.isSuccess) res.stdout else "unknown"
    }
}
