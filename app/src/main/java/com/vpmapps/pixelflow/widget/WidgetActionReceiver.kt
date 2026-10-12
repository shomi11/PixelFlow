package com.vpmapps.pixelflow.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.vpmapps.pixelflow.MainActivity
import com.vpmapps.pixelflow.data.local.AppDatabase
import com.vpmapps.pixelflow.service.RoutineExecutionEngine
import com.vpmapps.pixelflow.service.RoutineExecutionTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles run / stop / open taps from both home screen widgets.
 *
 * This receiver is declared with android:exported="false", so only PendingIntents created by this
 * app can reach it. The widget providers themselves must stay exported for the system to deliver
 * APPWIDGET_UPDATE, which is why the privileged run/stop actions no longer live on them.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WidgetActionReceiver"
        const val ACTION_RUN_ROUTINE = "com.vpmapps.pixelflow.widget.ACTION_RUN_ROUTINE"
        const val ACTION_STOP_ROUTINE = "com.vpmapps.pixelflow.widget.ACTION_STOP_ROUTINE"
        const val ACTION_OPEN_ROUTINE = "com.vpmapps.pixelflow.widget.ACTION_OPEN_ROUTINE"
        const val ACTION_REFRESH = "com.vpmapps.pixelflow.widget.ACTION_REFRESH"
        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_SOURCE = "extra_source"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: "Widget"

        when (action) {
            ACTION_REFRESH -> WidgetUpdateHelper.updateAllWidgets(context)
            ACTION_OPEN_ROUTINE -> {
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EXTRA_ROUTINE_ID", routineId)
                }
                context.startActivity(openIntent)
            }
            ACTION_RUN_ROUTINE, ACTION_STOP_ROUTINE -> {
                if (routineId <= 0) return
                runAsync(context, routineId, isStop = action == ACTION_STOP_ROUTINE, source = source)
            }
        }
    }

    private fun runAsync(context: Context, routineId: Long, isStop: Boolean, source: String) {
        val appContext = context.applicationContext
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PixelRoutines:WidgetActionWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(60_000L)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(appContext).routineDao()
                val routine = dao.getRoutineById(routineId)
                if (routine != null) {
                    val engine = RoutineExecutionEngine(appContext)
                    if (isStop) {
                        engine.restoreRoutineSettings(routine, "Stopped via $source")
                        RoutineExecutionTracker.markFinished(routineId)
                        dao.updateExecutionStatus(routineId, System.currentTimeMillis(), "IDLE")
                    } else {
                        engine.executeRoutine(routine, "$source Quick Run")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling widget action for routine $routineId", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                } catch (e: Throwable) {
                    // Ignore
                }
                WidgetUpdateHelper.updateAllWidgets(appContext)
                pendingResult.finish()
            }
        }
    }
}
