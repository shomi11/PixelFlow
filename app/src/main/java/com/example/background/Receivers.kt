package com.example.background

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.TriggerType
import com.example.service.RoutineExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Device rebooted. Setting up routine background worker and precision alarms.")
            WorkManagerHelper.setupPeriodicMonitoring(context)
            RoutineAlarmManager.rescheduleAllRoutines(context)
            GeofenceManager.registerAllGeofences(context)
        }
    }
}

class BatteryTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isPowerConnected = action == Intent.ACTION_POWER_CONNECTED
        val isPowerDisconnected = action == Intent.ACTION_POWER_DISCONNECTED
        val isBatteryLow = action == Intent.ACTION_BATTERY_LOW

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else -1

        val prefs = context.getSharedPreferences("trigger_state_prefs", Context.MODE_PRIVATE)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeRoutines = db.routineDao().getActiveRoutines()
                    .filter { it.triggerType == TriggerType.BATTERY }
                val engine = RoutineExecutionEngine(context)

                for (routine in activeRoutines) {
                    val config = JSONObject(routine.triggerConfigJson)
                    val reqCharging = config.optBoolean("charging", false)
                    val targetLevel = config.optInt("level", 20)

                    val lastChargedKey = "last_charging_${routine.id}"
                    val lastTriggeredKey = "last_battery_triggered_${routine.id}"
                    val wasCharging = prefs.getBoolean(lastChargedKey, false)

                    if (reqCharging) {
                        // Only trigger when charger is plugged in (transition from unplugged to plugged)
                        if (isPowerConnected && !wasCharging) {
                            prefs.edit().putBoolean(lastChargedKey, true).apply()
                            engine.executeRoutine(routine, "Charging Connected")
                        } else if (isPowerDisconnected) {
                            prefs.edit().putBoolean(lastChargedKey, false).apply()
                        }
                    } else {
                        // Battery level trigger: only trigger on transition below target level
                        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000L)
                        val lastTriggeredTime = prefs.getLong(lastTriggeredKey, 0L)

                        val isBelowThreshold = batteryPct in 1..targetLevel || isBatteryLow
                        if (isBelowThreshold && !isPowerConnected && lastTriggeredTime < oneHourAgo) {
                            prefs.edit().putLong(lastTriggeredKey, System.currentTimeMillis()).apply()
                            engine.executeRoutine(routine, "Battery Low ($batteryPct%)")
                        } else if (batteryPct > targetLevel + 5) {
                            // Reset threshold trigger when battery charges back above target + 5%
                            prefs.edit().putLong(lastTriggeredKey, 0L).apply()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BatteryTriggerReceiver", "Error evaluating battery triggers", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class WifiTriggerReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != WifiManager.NETWORK_STATE_CHANGED_ACTION) return

        val networkInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
        } else {
            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
        }
        val isConnected = networkInfo?.isConnected == true

        val wifiInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO, WifiInfo::class.java)
        } else {
            intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO)
        }
        val currentSsid = wifiInfo?.ssid?.trim('"') ?: ""

        val prefs = context.getSharedPreferences("trigger_state_prefs", Context.MODE_PRIVATE)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeRoutines = db.routineDao().getActiveRoutines()
                    .filter { it.triggerType == TriggerType.WIFI }
                val engine = RoutineExecutionEngine(context)

                for (routine in activeRoutines) {
                    val config = JSONObject(routine.triggerConfigJson)
                    val reqConnected = config.optBoolean("connected", true)
                    val targetSsid = config.optString("ssid", "").trim('"')

                    val lastStateKey = "wifi_last_connected_${routine.id}"
                    val lastSsidKey = "wifi_last_ssid_${routine.id}"
                    val wasConnected = prefs.getBoolean(lastStateKey, false)
                    val lastSsid = prefs.getString(lastSsidKey, "") ?: ""

                    // Check if target SSID matches
                    val isTargetNetwork = targetSsid.isBlank() ||
                            targetSsid.equals("Any Network", ignoreCase = true) ||
                            currentSsid.equals(targetSsid, ignoreCase = true)

                    if (reqConnected) {
                        // Trigger only when transitioning from disconnected to connected on target network
                        if (isConnected && isTargetNetwork && (!wasConnected || lastSsid != currentSsid)) {
                            prefs.edit()
                                .putBoolean(lastStateKey, true)
                                .putString(lastSsidKey, currentSsid)
                                .apply()
                            engine.executeRoutine(routine, "Wi-Fi: Connected to $currentSsid")
                        } else if (!isConnected) {
                            prefs.edit().putBoolean(lastStateKey, false).putString(lastSsidKey, "").apply()
                        }
                    } else {
                        // Disconnect trigger: only when transitioning from connected to disconnected
                        if (!isConnected && wasConnected) {
                            prefs.edit().putBoolean(lastStateKey, false).apply()
                            engine.executeRoutine(routine, "Wi-Fi: Disconnected")
                        } else if (isConnected) {
                            prefs.edit().putBoolean(lastStateKey, true).putString(lastSsidKey, currentSsid).apply()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WifiTriggerReceiver", "Error processing wifi trigger", e)
            } finally {
                pendingResult.finish()
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

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeRoutines = db.routineDao().getActiveRoutines()
                    .filter { it.triggerType == TriggerType.BLUETOOTH }
                val engine = RoutineExecutionEngine(context)

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
                        } else if (!reqConnected && isDisconnected && wasBtConnected && lastTriggeredTime < thirtySecondsAgo) {
                            prefs.edit()
                                .putBoolean(lastBtStateKey, false)
                                .putLong(lastTriggeredKey, System.currentTimeMillis())
                                .apply()
                            engine.executeRoutine(routine, "Bluetooth: Disconnected $deviceName")
                        } else if (isDisconnected) {
                            prefs.edit().putBoolean(lastBtStateKey, false).apply()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothReceiver", "Error processing bluetooth trigger", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
