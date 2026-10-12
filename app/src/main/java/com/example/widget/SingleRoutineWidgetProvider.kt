package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.Routine
import com.example.service.RoutineExecutionTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SingleRoutineWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "SingleRoutineWidget"
        const val PREFS_NAME = "single_routine_widget_prefs"
        const val PREF_PREFIX_KEY = "appwidget_routine_"

        const val EXTRA_ROUTINE_ID = WidgetActionReceiver.EXTRA_ROUTINE_ID

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

        private fun widgetActionIntent(context: Context, action: String, routineId: Long, appWidgetId: Int): Intent {
            return Intent(context, WidgetActionReceiver::class.java).apply {
                this.action = action
                putExtra(WidgetActionReceiver.EXTRA_ROUTINE_ID, routineId)
                putExtra(WidgetActionReceiver.EXTRA_SOURCE, "Quick Control Widget")
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
        }

        fun deleteRoutinePref(context: Context, appWidgetId: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_PREFIX_KEY + appWidgetId)
                .apply()
        }

        /**
         * Fire-and-forget refresh for callers on the main thread (e.g. the configure activity).
         */
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                updateAppWidgetNow(appContext, appWidgetManager, appWidgetId)
            }
        }

        /**
         * Loads the routine off the main thread and pushes the RemoteViews.
         */
        suspend fun updateAppWidgetNow(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            var routineId = loadRoutinePref(context, appWidgetId)

            val routine: Routine? = withContext(Dispatchers.IO) {
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

                    val stopIntent = widgetActionIntent(context, WidgetActionReceiver.ACTION_STOP_ROUTINE, routine.id, appWidgetId)
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

                    val runIntent = widgetActionIntent(context, WidgetActionReceiver.ACTION_RUN_ROUTINE, routine.id, appWidgetId)
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

                    val runIntent = widgetActionIntent(context, WidgetActionReceiver.ACTION_RUN_ROUTINE, routine.id, appWidgetId)
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
        // Database reads must not block the main thread, so finish the broadcast asynchronously.
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidgetNow(appContext, appWidgetManager, appWidgetId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating quick control widgets", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            deleteRoutinePref(context, appWidgetId)
        }
        super.onDeleted(context, appWidgetIds)
    }
}
