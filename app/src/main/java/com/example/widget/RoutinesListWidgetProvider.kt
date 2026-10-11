package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.service.RoutineExecutionEngine
import com.example.service.RoutineExecutionTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoutinesListWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "RoutinesListWidget"
        const val ACTION_WIDGET_RUN_ROUTINE = "com.example.ACTION_WIDGET_RUN_ROUTINE"
        const val ACTION_WIDGET_STOP_ROUTINE = "com.example.ACTION_WIDGET_STOP_ROUTINE"
        const val ACTION_WIDGET_REFRESH = "com.example.ACTION_WIDGET_REFRESH"
        const val ACTION_OPEN_ROUTINE = "com.example.ACTION_OPEN_ROUTINE"
        const val EXTRA_ROUTINE_ID = "extra_routine_id"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val serviceIntent = Intent(context, RoutinesListWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.fromParts("content", "widget_$appWidgetId", null)
            }

            val views = RemoteViews(context.packageName, R.layout.widget_routines_list).apply {
                setRemoteAdapter(R.id.widget_list_view, serviceIntent)
                setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

                // Refresh button click
                val refreshIntent = Intent(context, RoutinesListWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_REFRESH
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    90001,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

                // Add button click -> Open CreateRoutine in MainActivity
                val addIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EXTRA_ACTION", "CREATE_ROUTINE")
                }
                val addPendingIntent = PendingIntent.getActivity(
                    context,
                    90002,
                    addIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_btn_add, addPendingIntent)

                // Template PendingIntent for items in the ListView
                val itemClickIntent = Intent(context, RoutinesListWidgetProvider::class.java)
                val itemClickPendingIntent = PendingIntent.getBroadcast(
                    context,
                    90003,
                    itemClickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                setPendingIntentTemplate(R.id.widget_list_view, itemClickPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            ACTION_WIDGET_RUN_ROUTINE -> {
                val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
                if (routineId > 0) {
                    executeRoutineAsync(context, routineId)
                }
            }
            ACTION_WIDGET_STOP_ROUTINE -> {
                val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
                if (routineId > 0) {
                    stopRoutineAsync(context, routineId)
                }
            }
            ACTION_WIDGET_REFRESH -> {
                WidgetUpdateHelper.updateAllWidgets(context)
            }
            ACTION_OPEN_ROUTINE -> {
                val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EXTRA_ROUTINE_ID", routineId)
                }
                context.startActivity(openIntent)
            }
        }
    }

    private fun executeRoutineAsync(context: Context, routineId: Long) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PixelRoutines:WidgetRunWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(60_000L)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val routine = db.routineDao().getRoutineById(routineId)
                if (routine != null) {
                    val engine = RoutineExecutionEngine(context.applicationContext)
                    engine.executeRoutine(routine, "Widget Quick Run")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing routine from widget", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                } catch (e: Throwable) {
                    // Ignore
                }
                WidgetUpdateHelper.updateAllWidgets(context)
            }
        }
    }

    private fun stopRoutineAsync(context: Context, routineId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val routine = db.routineDao().getRoutineById(routineId)
                if (routine != null) {
                    val engine = RoutineExecutionEngine(context.applicationContext)
                    engine.restoreRoutineSettings(routine, "Stopped via Routines List Widget")
                }
                RoutineExecutionTracker.markFinished(routineId)
                db.routineDao().updateExecutionStatus(routineId, System.currentTimeMillis(), "IDLE")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping routine from widget", e)
            } finally {
                WidgetUpdateHelper.updateAllWidgets(context)
            }
        }
    }
}
