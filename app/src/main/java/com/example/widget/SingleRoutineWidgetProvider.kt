package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.PowerManager
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.Routine
import com.example.service.RoutineExecutionEngine
import com.example.service.RoutineExecutionTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SingleRoutineWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "SingleRoutineWidget"
        const val PREFS_NAME = "single_routine_widget_prefs"
        const val PREF_PREFIX_KEY = "appwidget_routine_"

        const val ACTION_WIDGET_RUN_ROUTINE = "com.example.ACTION_SINGLE_WIDGET_RUN_ROUTINE"
        const val ACTION_WIDGET_STOP_ROUTINE = "com.example.ACTION_SINGLE_WIDGET_STOP_ROUTINE"
        const val EXTRA_ROUTINE_ID = "extra_routine_id"

        fun saveRoutinePref(context: Context, appWidgetId: Int, routineId: Long) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(PREF_PREFIX_KEY + appWidgetId, routineId)
                .apply()
        }

        fun loadRoutinePref(context: Context, appWidgetId: Int): Long {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(PREF_PREFIX_KEY + appWidgetId, -1L)
        }

        fun deleteRoutinePref(context: Context, appWidgetId: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_PREFIX_KEY + appWidgetId)
                .apply()
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            var routineId = loadRoutinePref(context, appWidgetId)

            val routine: Routine? = runBlocking(Dispatchers.IO) {
                try {
                    val repo = com.example.data.repository.RoutineRepository.getInstance(context)
                    if (routineId > 0) {
                        repo.getRoutineById(routineId)
                    } else {
                        val db = AppDatabase.getDatabase(context)
                        val all = db.routineDao().getAllRoutinesList()
                        val first = all.firstOrNull { it.isEnabled } ?: all.firstOrNull()
                        if (first != null) {
                            saveRoutinePref(context, appWidgetId, first.id)
                            routineId = first.id
                            first
                        } else null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            val views = RemoteViews(context.packageName, R.layout.widget_single_routine)

            if (routine != null) {
                views.setTextViewText(R.id.single_widget_title, routine.title)
                views.setTextViewText(R.id.single_widget_trigger, routine.getTriggerSummary())

                val actionsSummary = routine.actions.joinToString(" • ") { it.toSummary() }
                views.setTextViewText(
                    R.id.single_widget_actions,
                    if (actionsSummary.isNotBlank()) actionsSummary else "No actions configured"
                )

                val isExecuting = RoutineExecutionTracker.isRunning(routine.id) || routine.lastExecutionStatus == "RUNNING"
                val isInActiveSchedule = routine.isCurrentlyInActiveWindow()
                val isCurrentlyRunning = isExecuting || isInActiveSchedule

                if (isCurrentlyRunning) {
                    views.setTextViewText(R.id.single_widget_badge, "RUNNING")
                    views.setInt(R.id.single_widget_badge, "setBackgroundResource", R.drawable.widget_badge_running_bg)
                    views.setTextColor(R.id.single_widget_badge, Color.parseColor("#00BFA5"))

                    views.setTextViewText(R.id.single_widget_btn_action, "TURN OFF / STOP")
                    views.setInt(R.id.single_widget_btn_action, "setBackgroundResource", R.drawable.widget_btn_stop_bg)

                    val stopIntent = Intent(context, SingleRoutineWidgetProvider::class.java).apply {
                        action = ACTION_WIDGET_STOP_ROUTINE
                        putExtra(EXTRA_ROUTINE_ID, routine.id)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val stopPendingIntent = PendingIntent.getBroadcast(
                        context,
                        (appWidgetId * 100) + 1,
                        stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.single_widget_btn_action, stopPendingIntent)
                } else if (routine.isEnabled) {
                    views.setTextViewText(R.id.single_widget_badge, "IDLE")
                    views.setInt(R.id.single_widget_badge, "setBackgroundResource", R.drawable.widget_badge_idle_bg)
                    views.setTextColor(R.id.single_widget_badge, Color.parseColor("#60A5FA"))

                    views.setTextViewText(R.id.single_widget_btn_action, "TURN ON / RUN")
                    views.setInt(R.id.single_widget_btn_action, "setBackgroundResource", R.drawable.widget_btn_run_bg)

                    val runIntent = Intent(context, SingleRoutineWidgetProvider::class.java).apply {
                        action = ACTION_WIDGET_RUN_ROUTINE
                        putExtra(EXTRA_ROUTINE_ID, routine.id)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val runPendingIntent = PendingIntent.getBroadcast(
                        context,
                        (appWidgetId * 100) + 2,
                        runIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.single_widget_btn_action, runPendingIntent)
                } else {
                    views.setTextViewText(R.id.single_widget_badge, "OFF")
                    views.setInt(R.id.single_widget_badge, "setBackgroundResource", R.drawable.widget_badge_disabled_bg)
                    views.setTextColor(R.id.single_widget_badge, Color.parseColor("#94A3B8"))

                    views.setTextViewText(R.id.single_widget_btn_action, "ENABLE & RUN")
                    views.setInt(R.id.single_widget_btn_action, "setBackgroundResource", R.drawable.widget_btn_run_bg)

                    val runIntent = Intent(context, SingleRoutineWidgetProvider::class.java).apply {
                        action = ACTION_WIDGET_RUN_ROUTINE
                        putExtra(EXTRA_ROUTINE_ID, routine.id)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val runPendingIntent = PendingIntent.getBroadcast(
                        context,
                        (appWidgetId * 100) + 3,
                        runIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.single_widget_btn_action, runPendingIntent)
                }

                // Header click opens routine in MainActivity
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EXTRA_ROUTINE_ID", routine.id)
                }
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    (appWidgetId * 100) + 4,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.single_widget_container, openPendingIntent)
            } else {
                // Setup / configure view
                views.setTextViewText(R.id.single_widget_title, "Configure Routine")
                views.setTextViewText(R.id.single_widget_trigger, "Tap to select routine")
                views.setTextViewText(R.id.single_widget_actions, "Choose which routine to display and control.")
                views.setTextViewText(R.id.single_widget_badge, "SETUP")
                views.setInt(R.id.single_widget_badge, "setBackgroundResource", R.drawable.widget_badge_disabled_bg)
                views.setTextViewText(R.id.single_widget_btn_action, "SELECT ROUTINE")

                val configIntent = Intent(context, SingleRoutineWidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val configPendingIntent = PendingIntent.getActivity(
                    context,
                    (appWidgetId * 100) + 5,
                    configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.single_widget_btn_action, configPendingIntent)
                views.setOnClickPendingIntent(R.id.single_widget_container, configPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            deleteRoutinePref(context, appWidgetId)
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            ACTION_WIDGET_RUN_ROUTINE -> {
                val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (routineId > 0) {
                    executeRoutineAsync(context, routineId, appWidgetId)
                }
            }
            ACTION_WIDGET_STOP_ROUTINE -> {
                val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (routineId > 0) {
                    stopRoutineAsync(context, routineId, appWidgetId)
                }
            }
        }
    }

    private fun executeRoutineAsync(context: Context, routineId: Long, appWidgetId: Int) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PixelRoutines:SingleWidgetRunWakeLock"
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
                    engine.executeRoutine(routine, "Single Widget Quick Run")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing routine from single widget", e)
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

    private fun stopRoutineAsync(context: Context, routineId: Long, appWidgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val routine = db.routineDao().getRoutineById(routineId)
                if (routine != null) {
                    val engine = RoutineExecutionEngine(context.applicationContext)
                    engine.restoreRoutineSettings(routine, "Stopped via Quick Control Widget")
                }
                RoutineExecutionTracker.markFinished(routineId)
                db.routineDao().updateExecutionStatus(routineId, System.currentTimeMillis(), "IDLE")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping routine from single widget", e)
            } finally {
                WidgetUpdateHelper.updateAllWidgets(context)
            }
        }
    }
}
