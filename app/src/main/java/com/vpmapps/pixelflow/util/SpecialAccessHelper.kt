package com.vpmapps.pixelflow.util

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.vpmapps.pixelflow.background.RoutineAlarmManager
import com.vpmapps.pixelflow.shizuku.ShizukuManager

/**
 * Checks and requests the special (settings-screen) permissions that routines depend on:
 * exact alarms for timed routines, usage access for "App opened" routines, and background
 * location for geofence routines.
 */
object SpecialAccessHelper {

    private const val TAG = "SpecialAccessHelper"

    fun canScheduleExactAlarms(context: Context): Boolean =
        RoutineAlarmManager.canScheduleExactAlarms(context)

    /** Opens the "Alarms & reminders" screen for this app (Android 12+). */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startSafely(context, intent)
    }

    fun hasUsageAccess(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            if (mode == AppOpsManager.MODE_DEFAULT) {
                context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
            } else {
                mode == AppOpsManager.MODE_ALLOWED
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Grants usage access silently through Shizuku when it is authorized. */
    suspend fun grantUsageAccessViaShizuku(context: Context): Boolean {
        if (hasUsageAccess(context)) return true
        if (!ShizukuManager.isAuthorized()) return false
        val result = ShizukuManager.executeCommand("appops set ${context.packageName} GET_USAGE_STATS allow")
        if (!result.isSuccess) Log.w(TAG, "Granting usage access via Shizuku failed: ${result.stderr}")
        return hasUsageAccess(context)
    }

    fun openUsageAccessSettings(context: Context) {
        startSafely(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun hasBackgroundLocation(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return LocationHelper.hasLocationPermission(context)
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun startSafely(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Specific settings screen unavailable, opening app details", e)
            try {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (ignored: Exception) {
                // Nothing else to open
            }
        }
    }
}
