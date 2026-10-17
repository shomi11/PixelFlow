package com.vpmapps.pixelflow.service

import android.app.UiModeManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.vpmapps.pixelflow.data.model.ActionType
import com.vpmapps.pixelflow.data.model.RoutineActionItem
import com.vpmapps.pixelflow.shizuku.ShizukuManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * RoutineStateSnapshotManager records and manages snapshots of system and hardware
 * settings prior to routine action execution, enabling full restoration to previous
 * states when the routine condition ends (e.g., Bluetooth disconnected, Wi-Fi lost,
 * schedule window completed, or manual stop).
 */
object RoutineStateSnapshotManager {
    private const val TAG = "RoutineStateSnapshotMgr"
    private const val PREFS_NAME = "routine_state_snapshots_prefs"
    private const val KEY_PREFIX_SNAPSHOT = "snapshot_"
    private const val KEY_PREFIX_TIMESTAMP = "snapshot_time_"
    private const val KEY_PREFIX_REASON = "snapshot_reason_"

    /**
     * Checks if an active state snapshot exists for the given routine ID.
     */
    fun hasSnapshot(context: Context, routineId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_PREFIX_SNAPSHOT + routineId)
    }

    /**
     * Retrieves the saved action items snapshot for the routine, representing
     * the hardware/system settings as they were before the routine was applied.
     */
    fun getSnapshot(context: Context, routineId: Long): List<RoutineActionItem>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_PREFIX_SNAPSHOT + routineId, null) ?: return null
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<RoutineActionItem>()
            for (i in 0 until array.length()) {
                list.add(RoutineActionItem.fromJsonObject(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing saved snapshot for routine $routineId", e)
            null
        }
    }

    /**
     * Saves a snapshot of settings only if one does not already exist,
     * preserving the original pre-routine state even if routine is re-evaluated.
     */
    fun saveSnapshotIfNotExists(
        context: Context,
        routineId: Long,
        snapshotActions: List<RoutineActionItem>,
        reason: String
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_PREFIX_SNAPSHOT + routineId)) {
            Log.d(TAG, "Snapshot already exists for routine $routineId, keeping original pre-routine settings")
            return false
        }
        val array = JSONArray()
        snapshotActions.forEach { array.put(it.toJsonObject()) }
        prefs.edit()
            .putString(KEY_PREFIX_SNAPSHOT + routineId, array.toString())
            .putLong(KEY_PREFIX_TIMESTAMP + routineId, System.currentTimeMillis())
            .putString(KEY_PREFIX_REASON + routineId, reason)
            .apply()
        Log.i(TAG, "Saved pre-routine settings snapshot for routine $routineId (${snapshotActions.size} items, reason: $reason)")
        return true
    }

    /**
     * Clears the saved snapshot for a routine once it has restored or reset.
     */
    fun clearSnapshot(context: Context, routineId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PREFIX_SNAPSHOT + routineId)
            .remove(KEY_PREFIX_TIMESTAMP + routineId)
            .remove(KEY_PREFIX_REASON + routineId)
            .apply()
        Log.i(TAG, "Cleared pre-routine settings snapshot for routine $routineId")
    }

    /**
     * Queries the actual current hardware and system settings corresponding to the actions
     * that the routine is about to modify.
     */
    suspend fun captureCurrentSettingsForActions(
        context: Context,
        actions: List<RoutineActionItem>
    ): List<RoutineActionItem> {
        val snapshot = mutableListOf<RoutineActionItem>()
        val appContext = context.applicationContext

        for (action in actions) {
            try {
                val preActionItem = when (action.type) {
                    ActionType.AOD -> {
                        val currentAod = queryCurrentAod(appContext)
                        RoutineActionItem(type = ActionType.AOD, enabledState = currentAod)
                    }
                    ActionType.BATTERY_SAVER -> {
                        val currentSaver = queryCurrentBatterySaver(appContext)
                        RoutineActionItem(type = ActionType.BATTERY_SAVER, enabledState = currentSaver)
                    }
                    ActionType.AUTO_ROTATE -> {
                        val currentRotate = queryCurrentAutoRotate(appContext)
                        RoutineActionItem(type = ActionType.AUTO_ROTATE, enabledState = currentRotate)
                    }
                    ActionType.DARK_MODE -> {
                        val currentDark = queryCurrentDarkMode(appContext)
                        RoutineActionItem(type = ActionType.DARK_MODE, enabledState = currentDark)
                    }
                    ActionType.SOUND_PROFILE -> {
                        val (mode, vol) = queryCurrentSoundProfile(appContext)
                        RoutineActionItem(type = ActionType.SOUND_PROFILE, stringValue = mode, intValue = vol)
                    }
                    ActionType.WIFI -> {
                        val currentWifi = queryCurrentWifi(appContext)
                        RoutineActionItem(type = ActionType.WIFI, enabledState = currentWifi)
                    }
                    ActionType.HOTSPOT -> {
                        // Restore hotspot by turning it off
                        RoutineActionItem(type = ActionType.HOTSPOT, enabledState = false)
                    }
                    ActionType.BLUETOOTH -> {
                        val currentBt = queryCurrentBluetooth(appContext)
                        RoutineActionItem(type = ActionType.BLUETOOTH, enabledState = currentBt)
                    }
                    ActionType.SCREEN_TIMEOUT -> {
                        val currentTimeout = queryCurrentScreenTimeout(appContext)
                        RoutineActionItem(type = ActionType.SCREEN_TIMEOUT, intValue = currentTimeout)
                    }
                    ActionType.CUSTOM_NOTIFICATION -> null
                }
                if (preActionItem != null) {
                    snapshot.add(preActionItem)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed capturing state for ${action.type}", e)
            }
        }
        return snapshot
    }

    private suspend fun queryCurrentAod(context: Context): Boolean {
        return try {
            val value = Settings.Secure.getInt(context.contentResolver, "doze_always_on", 1)
            value == 1
        } catch (e: Exception) {
            val res = ShizukuManager.executeCommand("settings get secure doze_always_on")
            res.stdout.trim() == "1"
        }
    }

    private suspend fun queryCurrentBatterySaver(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager != null) {
            return powerManager.isPowerSaveMode
        }
        return try {
            Settings.Global.getInt(context.contentResolver, "low_power", 0) == 1
        } catch (e: Exception) {
            val res = ShizukuManager.executeCommand("settings get global low_power")
            res.stdout.trim() == "1"
        }
    }

    private suspend fun queryCurrentAutoRotate(context: Context): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        } catch (e: Exception) {
            val res = ShizukuManager.executeCommand("settings get system accelerometer_rotation")
            res.stdout.trim() == "1"
        }
    }

    private suspend fun queryCurrentDarkMode(context: Context): Boolean {
        return try {
            // nightMode is the system setting, not PixelFlow's own theme. For AUTO/CUSTOM schedules it
            // doesn't say whether dark is active right now, so read the current configuration instead.
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            when (uiModeManager?.nightMode) {
                UiModeManager.MODE_NIGHT_YES -> true
                UiModeManager.MODE_NIGHT_NO -> false
                else -> {
                    val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    mode == Configuration.UI_MODE_NIGHT_YES
                }
            }
        } catch (e: Exception) {
            val res = ShizukuManager.executeCommand("settings get secure ui_night_mode")
            res.stdout.trim() == "2"
        }
    }

    private fun queryCurrentSoundProfile(context: Context): Pair<String, Int> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val mode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "SILENT"
            AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
            else -> "NORMAL"
        }
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val volPct = if (maxVol > 0) (curVol * 100 / maxVol) else -1
        return Pair(mode, volPct)
    }

    private suspend fun queryCurrentWifi(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        if (wifiManager != null) {
            return wifiManager.isWifiEnabled
        }
        return try {
            Settings.Global.getInt(context.contentResolver, "wifi_on", 1) == 1
        } catch (e: Exception) {
            val res = ShizukuManager.executeCommand("settings get global wifi_on")
            res.stdout.trim() == "1"
        }
    }

    private suspend fun queryCurrentBluetooth(context: Context): Boolean {
        @Suppress("DEPRECATION")
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        @Suppress("DEPRECATION")
        if (btAdapter != null) {
            return btAdapter.isEnabled
        }
        return try {
            Settings.Global.getInt(context.contentResolver, "bluetooth_on", 1) == 1
        } catch (e: Exception) {
            val res = ShizukuManager.executeCommand("settings get global bluetooth_on")
            res.stdout.trim() == "1"
        }
    }

    private suspend fun queryCurrentScreenTimeout(context: Context): Int {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 60000)
        } catch (e: Exception) {
            val res = ShizukuManager.executeCommand("settings get system screen_off_timeout")
            res.stdout.trim().toIntOrNull() ?: 60000
        }
    }
}
