package com.example.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

data class SavedBluetoothDevice(
    val name: String,
    val address: String
)

object BluetoothHelper {
    private const val TAG = "BluetoothHelper"

    /**
     * Checks if the app has required Bluetooth permissions to access bonded devices.
     */
    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Retrieves the list of currently bonded / paired Bluetooth devices on this device.
     */
    fun getSavedBluetoothDevices(context: Context): List<SavedBluetoothDevice> {
        if (!hasBluetoothPermission(context)) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
            return emptyList()
        }

        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            @Suppress("DEPRECATION")
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

            if (adapter == null || !adapter.isEnabled) {
                Log.i(TAG, "Bluetooth adapter is null or disabled")
                return emptyList()
            }

            val bonded = adapter.bondedDevices ?: emptySet()
            bonded.map { device ->
                val deviceName = try {
                    device.name?.takeIf { it.isNotBlank() } ?: device.address
                } catch (e: SecurityException) {
                    device.address
                }
                SavedBluetoothDevice(name = deviceName, address = device.address)
            }.sortedBy { it.name }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while accessing bonded devices: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching saved bluetooth devices: ${e.message}", e)
            emptyList()
        }
    }
}
