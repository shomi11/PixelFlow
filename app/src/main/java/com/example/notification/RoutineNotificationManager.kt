package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.RoutineActionItem
import com.example.data.model.RoutineEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RoutineNotificationManager manages system notifications alerting the user
 * whenever an automation routine or action triggers and executes.
 */
object RoutineNotificationManager {

    private const val TAG = "RoutineNotificationMgr"
    const val CHANNEL_ID = "routine_executions_channel"
    private const val PREFS_NAME = "pixel_routines_prefs"
    private const val KEY_NOTIFICATIONS_ENABLED = "routine_notifications_enabled"

    fun init(context: Context) {
        createNotificationChannel(context)
    }

    /**
     * Creates the Android Notification Channel required on Android 8.0+ (API 26+).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Routine Executions"
            val descriptionText = "Notifications when automation routines and actions execute"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if routine notifications are enabled by the user in app settings.
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    /**
     * Enables or disables routine execution notifications in app settings.
     */
    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    /**
     * Posts a notification detailing the routine execution and actions performed.
     */
    fun notifyRoutineExecuted(
        context: Context,
        routine: RoutineEntity,
        actions: List<RoutineActionItem>,
        triggerReason: String,
        isSuccess: Boolean,
        status: String
    ) {
        if (!areNotificationsEnabled(context)) {
            Log.d(TAG, "Routine notifications are disabled in user preferences")
            return
        }

        // On Android 13+ (API 33+), check runtime POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Cannot post notification: POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        try {
            createNotificationChannel(context)

            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val formattedTime = timeFormat.format(Date())

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("EXTRA_ROUTINE_ID", routine.id)
                putExtra("EXTRA_SHOW_LOGS", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                routine.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Compose action bullet summary
            val actionsDetails = if (actions.isNotEmpty()) {
                actions.joinToString("\n") { "• ${it.toSummary()}" }
            } else {
                "• No actions configured"
            }

            val bigTextContent = buildString {
                append("Trigger: $triggerReason\n")
                append("Time: $formattedTime\n\n")
                append("Actions Taken:\n")
                append(actionsDetails)
                append("\n\nStatus: $status")
            }

            val statusIconPrefix = if (isSuccess) "✓" else "⚠"
            val title = if (isSuccess) "$statusIconPrefix Routine Executed: ${routine.title}" else "⚠ Routine Execution Failed: ${routine.title}"
            val shortSummary = if (isSuccess) "${actions.size} action(s) applied • $status" else "Actions could not be applied • $status"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(shortSummary)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextContent))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .build()

            // Notification ID calculated so each routine updates its entry
            val notificationId = 10000 + (routine.id % 5000).toInt()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.i(TAG, "Notification posted for routine '${routine.title}' (ID: $notificationId)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post routine execution notification", e)
        }
    }

    /**
     * Sends a test notification to verify notification permissions and channel display.
     */
    fun sendTestNotification(context: Context) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Pixel Routines Notifications Active")
                .setContentText("You will receive alerts whenever automated routines trigger.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Automations will notify you when actions like Always-On Display, Volume, Vibrate, Silent Mode, and Battery Saver are applied.")
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(9999, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting test notification", e)
        }
    }
}
