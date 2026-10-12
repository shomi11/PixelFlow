package com.example.background

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.RoutineDao
import com.example.data.model.ActionType
import com.example.data.model.CustomNotificationConfig
import com.example.data.model.Routine
import com.example.data.model.RoutineActionItem
import com.example.data.model.RoutineTrigger
import com.example.data.model.TriggerType
import com.example.service.RoutineExecutionEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies that Battery, Wi-Fi and "App opened" triggers fire exactly once per real state change.
 * Routines use an immediate custom notification as their only action so no Shizuku is needed;
 * every execution writes one row to the execution log, which is what these tests count.
 */
@RunWith(RobolectricTestRunner::class)
class TriggerEvaluatorTest {

    private lateinit var context: Context
    private lateinit var dao: RoutineDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        resetDatabaseSingleton()
        dao = AppDatabase.getDatabase(context).routineDao()
        context.getSharedPreferences("trigger_state_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        resetDatabaseSingleton()
    }

    @Test
    fun chargingTrigger_firesOncePerPlugIn() = runBlocking {
        insertRoutine(TriggerType.BATTERY, JSONObject().put("charging", true).put("level", 20))

        TriggerEvaluator.onBatteryState(context, 50, isCharging = true)
        TriggerEvaluator.onBatteryState(context, 51, isCharging = true)
        assertEquals(1, executionCount())

        TriggerEvaluator.onBatteryState(context, 51, isCharging = false)
        TriggerEvaluator.onBatteryState(context, 51, isCharging = true)
        assertEquals(2, executionCount())
    }

    @Test
    fun batteryLevelTrigger_firesOnDropAndRearmsAfterRecharge() = runBlocking {
        insertRoutine(TriggerType.BATTERY, JSONObject().put("charging", false).put("level", 20))

        TriggerEvaluator.onBatteryState(context, 40, isCharging = false)
        assertEquals(0, executionCount())

        TriggerEvaluator.onBatteryState(context, 20, isCharging = false)
        TriggerEvaluator.onBatteryState(context, 15, isCharging = false)
        assertEquals(1, executionCount())

        TriggerEvaluator.onBatteryState(context, 30, isCharging = false)
        TriggerEvaluator.onBatteryState(context, 19, isCharging = false)
        assertEquals(2, executionCount())
    }

    @Test
    fun wifiTrigger_onlyMatchesTargetNetwork() = runBlocking {
        insertRoutine(TriggerType.WIFI, JSONObject().put("connected", true).put("ssid", "Home"))

        TriggerEvaluator.onWifiState(context, isConnected = true, ssid = "Office")
        TriggerEvaluator.onWifiState(context, isConnected = true, ssid = "")
        assertEquals(0, executionCount())

        TriggerEvaluator.onWifiState(context, isConnected = true, ssid = "Home")
        TriggerEvaluator.onWifiState(context, isConnected = true, ssid = "Home")
        assertEquals(1, executionCount())

        TriggerEvaluator.onWifiState(context, isConnected = false, ssid = "")
        TriggerEvaluator.onWifiState(context, isConnected = true, ssid = "Home")
        assertEquals(2, executionCount())
    }

    @Test
    fun wifiDisconnectTrigger_firesWhenLeavingNetwork() = runBlocking {
        insertRoutine(TriggerType.WIFI, JSONObject().put("connected", false).put("ssid", "Any Network"))

        TriggerEvaluator.onWifiState(context, isConnected = false, ssid = "")
        assertEquals(0, executionCount())

        TriggerEvaluator.onWifiState(context, isConnected = true, ssid = "Home")
        TriggerEvaluator.onWifiState(context, isConnected = false, ssid = "")
        TriggerEvaluator.onWifiState(context, isConnected = false, ssid = "")
        assertEquals(1, executionCount())
    }

    @Test
    fun appTrigger_firesWhenTargetAppComesToForeground() = runBlocking {
        insertRoutine(TriggerType.APP, JSONObject().put("appName", "Maps").put("packageName", "com.google.android.apps.maps"))

        TriggerEvaluator.onForegroundAppChanged(context, "com.android.launcher", "com.whatsapp")
        assertEquals(0, executionCount())

        TriggerEvaluator.onForegroundAppChanged(context, "com.whatsapp", "com.google.android.apps.maps")
        TriggerEvaluator.onForegroundAppChanged(context, "com.google.android.apps.maps", "com.android.launcher")
        assertEquals(1, executionCount())
    }

    @Test
    fun disabledRoutines_areIgnored() = runBlocking {
        insertRoutine(TriggerType.BATTERY, JSONObject().put("charging", true), enabled = false)
        TriggerEvaluator.onBatteryState(context, 50, isCharging = true)
        assertEquals(0, executionCount())
    }

    @Test
    fun sameRoutine_doesNotRunTwiceConcurrently() = runBlocking {
        val id = insertRoutine(TriggerType.TIME, JSONObject().put("startTime", "08:00"))
        val routine = dao.getRoutineById(id)!!
        val engine = RoutineExecutionEngine(context)

        val first = async { engine.executeRoutine(routine, "first") }
        val second = async { engine.executeRoutine(routine, "second") }
        val results = listOf(first.await(), second.await())

        assertEquals(1, executionCount())
        assertTrue(results.contains(false))
    }

    private suspend fun insertRoutine(type: TriggerType, config: JSONObject, enabled: Boolean = true): Long {
        val notification = CustomNotificationConfig(title = "Test", scheduleType = "IMMEDIATE")
        return dao.insertRoutine(
            Routine(
                name = "Test ${type.name}",
                isEnabled = enabled,
                triggers = listOf(RoutineTrigger(type = type, configJson = config.toString())),
                actions = listOf(RoutineActionItem(type = ActionType.CUSTOM_NOTIFICATION, stringValue = notification.toJsonString())),
                restoreOnEnd = false
            )
        )
    }

    private suspend fun executionCount(): Int = dao.getAllLogs().first().size

    private fun resetDatabaseSingleton() {
        val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        (field.get(null) as? AppDatabase)?.close()
        field.set(null, null)
    }
}
