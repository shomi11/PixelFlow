package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.Routine
import com.example.service.RoutineExecutionTracker
import kotlinx.coroutines.runBlocking

class RoutinesListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return RoutinesListRemoteViewsFactory(applicationContext)
    }
}

class RoutinesListRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var routinesList: List<Routine> = emptyList()

    override fun onCreate() {
        fetchRoutines()
    }

    override fun onDataSetChanged() {
        fetchRoutines()
    }

    private fun fetchRoutines() {
        try {
            val db = AppDatabase.getDatabase(context)
            routinesList = runBlocking {
                val allRoutines = db.routineDao().getAllRoutinesList()
                if (allRoutines.isNotEmpty()) allRoutines else db.routineDao().getActiveRoutines()
            }
        } catch (e: Exception) {
            routinesList = emptyList()
        }
    }

    override fun onDestroy() {
        routinesList = emptyList()
    }

    override fun getCount(): Int = routinesList.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= routinesList.size) return null
        val routine = routinesList[position]

        val views = RemoteViews(context.packageName, R.layout.widget_routine_list_item)

        views.setTextViewText(R.id.widget_item_title, routine.title)
        views.setTextViewText(R.id.widget_item_trigger_badge, routine.getTriggerSummary())

        val actionsSummary = routine.actions.joinToString(" • ") { it.toSummary() }
        views.setTextViewText(R.id.widget_item_subtitle, if (actionsSummary.isNotBlank()) actionsSummary else "No actions configured")

        val isExecuting = RoutineExecutionTracker.isRunning(routine.id) || routine.lastExecutionStatus == "RUNNING"
        val isInActiveSchedule = routine.isCurrentlyInActiveWindow()
        val isCurrentlyRunning = isExecuting || isInActiveSchedule

        if (isCurrentlyRunning) {
            views.setTextViewText(R.id.widget_item_status_badge, "RUNNING")
            views.setInt(R.id.widget_item_status_badge, "setBackgroundResource", R.drawable.widget_badge_running_bg)
            views.setTextColor(R.id.widget_item_status_badge, Color.parseColor("#00BFA5"))

            views.setTextViewText(R.id.widget_item_btn_action, "STOP")
            views.setInt(R.id.widget_item_btn_action, "setBackgroundResource", R.drawable.widget_btn_stop_bg)

            val actionFillInIntent = Intent().apply {
                action = RoutinesListWidgetProvider.ACTION_WIDGET_STOP_ROUTINE
                putExtra(RoutinesListWidgetProvider.EXTRA_ROUTINE_ID, routine.id)
            }
            views.setOnClickFillInIntent(R.id.widget_item_btn_action, actionFillInIntent)
        } else if (routine.isEnabled) {
            views.setTextViewText(R.id.widget_item_status_badge, "IDLE")
            views.setInt(R.id.widget_item_status_badge, "setBackgroundResource", R.drawable.widget_badge_idle_bg)
            views.setTextColor(R.id.widget_item_status_badge, Color.parseColor("#60A5FA"))

            views.setTextViewText(R.id.widget_item_btn_action, "RUN")
            views.setInt(R.id.widget_item_btn_action, "setBackgroundResource", R.drawable.widget_btn_run_bg)

            val actionFillInIntent = Intent().apply {
                action = RoutinesListWidgetProvider.ACTION_WIDGET_RUN_ROUTINE
                putExtra(RoutinesListWidgetProvider.EXTRA_ROUTINE_ID, routine.id)
            }
            views.setOnClickFillInIntent(R.id.widget_item_btn_action, actionFillInIntent)
        } else {
            views.setTextViewText(R.id.widget_item_status_badge, "OFF")
            views.setInt(R.id.widget_item_status_badge, "setBackgroundResource", R.drawable.widget_badge_disabled_bg)
            views.setTextColor(R.id.widget_item_status_badge, Color.parseColor("#94A3B8"))

            views.setTextViewText(R.id.widget_item_btn_action, "RUN")
            views.setInt(R.id.widget_item_btn_action, "setBackgroundResource", R.drawable.widget_btn_run_bg)

            val actionFillInIntent = Intent().apply {
                action = RoutinesListWidgetProvider.ACTION_WIDGET_RUN_ROUTINE
                putExtra(RoutinesListWidgetProvider.EXTRA_ROUTINE_ID, routine.id)
            }
            views.setOnClickFillInIntent(R.id.widget_item_btn_action, actionFillInIntent)
        }

        // Tapping container opens routine in app
        val containerFillInIntent = Intent().apply {
            action = RoutinesListWidgetProvider.ACTION_OPEN_ROUTINE
            putExtra(RoutinesListWidgetProvider.EXTRA_ROUTINE_ID, routine.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, containerFillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = routinesList.getOrNull(position)?.id ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
