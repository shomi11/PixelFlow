package com.vpmapps.pixelflow.background

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
                    // Nothing is connected right after boot; devices report themselves again on reconnect
                    DeviceFactsStore.clearBluetooth(context)
                }
                WorkManagerHelper.setupPeriodicMonitoring(context)
                RoutineAlarmManager.rescheduleAllRoutines(context)
                GeofenceManager.registerAllGeofences(context)
                TriggerMonitorService.refresh(context)
                TriggerEvaluator.evaluateAsync(context)
            }
        }
    }
}

/**
 * Records Bluetooth connections (Android still delivers ACL broadcasts to manifest receivers) and
 * re-checks routine conditions.
 */
class BluetoothTriggerReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        val connected = when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> true
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> false
            else -> return
        }
        val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        val address = device?.address ?: return
        val name = try { device.name ?: address } catch (e: SecurityException) { address }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TriggerEvaluator.onBluetoothChanged(appContext, name, address, connected)
            } catch (e: Exception) {
                Log.e("BluetoothReceiver", "Error processing bluetooth change", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
