package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean = false
)

object AppInfoHelper {

    fun getInstalledApps(context: Context): List<InstalledAppInfo> {
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }

            val apps = resolveList.mapNotNull { resolveInfo ->
                val label = resolveInfo.loadLabel(pm)?.toString()?.trim()
                val pkg = resolveInfo.activityInfo?.packageName
                if (!label.isNullOrBlank() && !pkg.isNullOrBlank()) {
                    val isSys = try {
                        val appInfo = resolveInfo.activityInfo.applicationInfo
                        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    } catch (e: Exception) {
                        false
                    }
                    InstalledAppInfo(
                        appName = label,
                        packageName = pkg,
                        isSystemApp = isSys
                    )
                } else null
            }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }

            if (apps.isNotEmpty()) apps else getFallbackApps()
        } catch (e: Exception) {
            getFallbackApps()
        }
    }

    fun getAppIconBitmap(context: Context, packageName: String): ImageBitmap? {
        return try {
            val pm = context.packageManager
            val drawable: Drawable = pm.getApplicationIcon(packageName)
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    private fun getFallbackApps(): List<InstalledAppInfo> {
        return listOf(
            InstalledAppInfo("YouTube", "com.google.android.youtube"),
            InstalledAppInfo("Google Maps", "com.google.android.apps.maps"),
            InstalledAppInfo("Google Chrome", "com.android.chrome"),
            InstalledAppInfo("Spotify", "com.spotify.music"),
            InstalledAppInfo("Camera", "com.google.android.GoogleCamera"),
            InstalledAppInfo("Clock", "com.google.android.deskclock"),
            InstalledAppInfo("Settings", "com.android.settings", isSystemApp = true),
            InstalledAppInfo("Gmail", "com.google.android.gm"),
            InstalledAppInfo("Netflix", "com.netflix.mediaclient"),
            InstalledAppInfo("Messages", "com.google.android.apps.messaging"),
            InstalledAppInfo("Google Photos", "com.google.android.apps.photos"),
            InstalledAppInfo("Phone", "com.google.android.dialer", isSystemApp = true),
            InstalledAppInfo("WhatsApp", "com.whatsapp"),
            InstalledAppInfo("Kindle", "com.amazon.kindle"),
            InstalledAppInfo("Slack", "com.Slack")
        ).sortedBy { it.appName }
    }
}
