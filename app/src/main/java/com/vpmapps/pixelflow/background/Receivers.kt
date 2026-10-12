package com.vpmapps.pixelflow.background

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vpmapps.pixelflow.data.local.AppDatabase
import com.vpmapps.pixelflow.data.model.TriggerType
import com.vpmapps.pixelflow.service.RoutineExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Re-arms everything the system forgets: alarms and geofences are cleared on reboot, alarm times
 * shift when the clock or time zone changes, and exact alarms are re-allowed when the user grants
 * "Alarms & reminders". Also the only background moments where Android 12+ lets us start the
 * trigger monitor service without the app being opened.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> {
                Log.d("BootReceiver", "Rescheduling routines after ${intent.action}")
                WorkManagerHelper.setupPeriodicMonitoring(context)
                RoutineAlarmManager.rescheduleAllRoutines(context)
                GeofenceManager.registerAllGeofences(context)
                TriggerMonitorService.refresh(context)
            }
        }
    }
}

class BluetoothTriggerReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isConnected = action == BluetoothDevice.ACTION_ACL_CONNECTED
        val isDisconnected = action == BluetoothDevice.ACTION_ACL_DISCONNECTED

        if (!isConnected && !isDisconnected) return

        val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        val deviceName = try { device?.name ?: "Unknown Device" } catch (e: SecurityException) { "Bluetooth Device" }

        val prefs = context.getSharedPreferences("trigger_state_prefs", Context.MODE_PRIVATE)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val wakeLock = powerManager?.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "PixelRoutines:BluetoothTriggerWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(60_000L)
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val activeRoutines = db.routineDao().getActiveRoutines()
                    .filter { it.triggerType == TriggerType.BLUETOOTH }
                val engine = RoutineExecutionEngine(appContext)

                for (routine in activeRoutines) {
                    val config = JSONObject(routine.triggerConfigJson)
                    val reqConnected = config.optBoolean("connected", true)
                    val targetDevice = config.optString("device", "").trim()

                    val lastBtStateKey = "bt_last_connected_${routine.id}"
                    val wasBtConnected = prefs.getBoolean(lastBtStateKey, false)
                    val lastTriggeredKey = "bt_last_time_${routine.id}"
                    val lastTriggeredTime = prefs.getLong(lastTriggeredKey, 0L)
                    val thirtySecondsAgo = System.currentTimeMillis() - 30_000L

                    val isTargetDevice = if (targetDevice.isBlank() ||
                        targetDevice.equals("Any Device", ignoreCase = true) ||
                        targetDevice.equals("Any Paired Device", ignoreCase = true)
                    ) {
                        true
                    } else {
                        deviceName.contains(targetDevice, ignoreCase = true) ||
                            (device?.address?.equals(targetDevice, ignoreCase = true) == true)
                    }

                    if (isTargetDevice) {
                        if (reqConnected && isConnected && !wasBtConnected && lastTriggeredTime < thirtySecondsAgo) {
                            prefs.edit()
                                .putBoolean(lastBtStateKey, true)
                                .putLong(lastTriggeredKey, System.currentTimeMillis())
                                .apply()
                            engine.executeRoutine(routine, "Bluetooth: Connected $deviceName")
                        } else if (reqConnected && isDisconnected && wasBtConnected) {
                            // Target device disconnected: ROUTINE HAS ENDED! Restore previous settings
                            prefs.edit().putBoolean(lastBtStateKey, false).apply()
                            if (routine.restoreOnEnd) {
                                engine.restoreRoutineSettings(routine, "Bluetooth Disconnected: $deviceName")
                            }
                        } else if (!reqConnected && isDisconnected && wasBtConnected && lastTriggeredTime < thirtySecondsAgo) {
                            prefs.edit()
                                .putBoolean(lastBtStateKey, false)
                                .putLong(lastTriggeredKey, System.currentTimeMillis())
                                .apply()
                            engine.executeRoutine(routine, "Bluetooth: Disconnected $deviceName")
                        } else if (!reqConnected && isConnected && !wasBtConnected) {
                            prefs.edit().putBoolean(lastBtStateKey, true).apply()
                            if (routine.restoreOnEnd) {
                                engine.restoreRoutineSettings(routine, "Bluetooth Reconnected: $deviceName")
                            }
                        } else if (isDisconnected) {
                            prefs.edit().putBoolean(lastBtStateKey, false).apply()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothReceiver", "Error processing bluetooth trigger", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                } catch (e: Throwable) {
                    // Ignore
                }
                com.vpmapps.pixelflow.widget.WidgetUpdateHelper.updateAllWidgets(appContext)
                pendingResult.finish()
            }
        }
    }
}
