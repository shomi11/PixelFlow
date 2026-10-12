package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class RoutinesListWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_RUN_ROUTINE = WidgetActionReceiver.ACTION_RUN_ROUTINE
        const val ACTION_WIDGET_STOP_ROUTINE = WidgetActionReceiver.ACTION_STOP_ROUTINE
        const val ACTION_OPEN_ROUTINE = WidgetActionReceiver.ACTION_OPEN_ROUTINE
        const val EXTRA_ROUTINE_ID = WidgetActionReceiver.EXTRA_ROUTINE_ID

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val serviceIntent = Intent(context, RoutinesListWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.fromParts("content", "widget_$appWidgetId", null)
            }

            val views = RemoteViews(context.packageName, R.layout.widget_routines_list).apply {
                setRemoteAdapter(R.id.widget_list_view, serviceIntent)
                setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

                // Refresh button click
                val refreshIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                    action = WidgetActionReceiver.ACTION_REFRESH
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

                // Template PendingIntent for items in the ListView. Items fill in the action and
                // routine id; the receiver is not exported, so other apps cannot send these.
                val itemClickIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                    putExtra(WidgetActionReceiver.EXTRA_SOURCE, "Routines List Widget")
                }
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
}
