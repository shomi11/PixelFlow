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
        }
    }
}

class BatteryTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isCharging = action == Intent.ACTION_POWER_CONNECTED
        val isDischarging = action == Intent.ACTION_POWER_DISCONNECTED

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else -1

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

                    val matches = if (reqCharging) {
                        isCharging
                    } else if (batteryPct > 0) {
                        batteryPct <= targetLevel && !isCharging
                    } else {
                        false
                    }

                    if (matches) {
                        engine.executeRoutine(routine, "Battery Event: $action")
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

                    val matches = if (reqConnected == isConnected) {
                        if (targetSsid.isBlank() || targetSsid.equals("Any Network", ignoreCase = true)) {
                            true
                        } else {
                            currentSsid.equals(targetSsid, ignoreCase = true)
                        }
                    } else {
                        false
                    }

                    if (matches) {
                        engine.executeRoutine(routine, "Wi-Fi: ${if (isConnected) "Connected to $currentSsid" else "Disconnected"}")
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

                    val matches = if (reqConnected == isConnected) {
                        if (targetDevice.isBlank() ||
                            targetDevice.equals("Any Device", ignoreCase = true) ||
                            targetDevice.equals("Any Paired Device", ignoreCase = true)
                        ) {
                            true
                        } else {
                            deviceName.contains(targetDevice, ignoreCase = true) ||
                                (device?.address?.equals(targetDevice, ignoreCase = true) == true)
                        }
                    } else {
                        false
                    }

                    if (matches) {
                        engine.executeRoutine(routine, "Bluetooth: ${if (isConnected) "Connected $deviceName" else "Disconnected $deviceName"}")
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
