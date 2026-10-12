package com.vpmapps.pixelflow.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vpmapps.pixelflow.R

object WidgetUpdateHelper {

    private const val TAG = "WidgetUpdateHelper"

    fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            // 1. Update List Widget
            val listWidgetComponent = ComponentName(context, RoutinesListWidgetProvider::class.java)
            val listWidgetIds = appWidgetManager.getAppWidgetIds(listWidgetComponent)
            if (listWidgetIds.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(listWidgetIds, R.id.widget_list_view)
                val listUpdateIntent = Intent(context, RoutinesListWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, listWidgetIds)
                }
                context.sendBroadcast(listUpdateIntent)
            }

            // 2. Update Single Routine Widget
            val singleWidgetComponent = ComponentName(context, SingleRoutineWidgetProvider::class.java)
            val singleWidgetIds = appWidgetManager.getAppWidgetIds(singleWidgetComponent)
            if (singleWidgetIds.isNotEmpty()) {
                val singleUpdateIntent = Intent(context, SingleRoutineWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, singleWidgetIds)
                }
                context.sendBroadcast(singleUpdateIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widgets", e)
        }
    }
}
