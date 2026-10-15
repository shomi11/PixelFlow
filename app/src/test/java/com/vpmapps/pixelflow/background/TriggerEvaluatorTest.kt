package com.vpmapps.pixelflow.background

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vpmapps.pixelflow.data.local.AppDatabase
import com.vpmapps.pixelflow.data.local.RoutineDao
import com.vpmapps.pixelflow.data.model.ActionType
import com.vpmapps.pixelflow.data.model.CustomNotificationConfig
import com.vpmapps.pixelflow.data.model.Routine
import com.vpmapps.pixelflow.data.model.RoutineActionItem
import com.vpmapps.pixelflow.data.model.RoutineTrigger
import com.vpmapps.pixelflow.data.model.TriggerType
import com.vpmapps.pixelflow.service.RoutineExecutionEngine
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
        listOf("device_facts", "routine_activation").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
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
    fun wifiDisconnectedCondition_isAState() = runBlocking {
        insertRoutine(TriggerType.WIFI, JSONObject().put("connected", false).put("ssid", "Any Network"))

        // "Not on Wi-Fi" is already true, so the routine starts right away
        TriggerEvaluator.onWifiState(context, isConnected = false, ssid = "")
        TriggerEvaluator.onWifiState(context, isConnected = false, ssid = "")
        assertEquals(1, executionCount())

        TriggerEvaluator.onWifiState(context, isConnected = true, ssid = "Home")
        TriggerEvaluator.onWifiState(context, isConnected = false, ssid = "")
        assertEquals(2, executionCount())
    }

    @Test
    fun wifiAndTimeOfDay_runsOnlyAtThatTimeOnThatNetwork() = runBlocking {
        val id = insertRoutine(
            listOf(
                RoutineTrigger(TriggerType.WIFI, JSONObject().put("connected", true).put("ssid", "Home").toString()),
                RoutineTrigger(TriggerType.TIME, JSONObject().put("scheduleMode", "DAILY").put("startTime", "21:00").toString())
            )
        )

        // Joining Home Wi-Fi alone doesn't run it: it waits for 9 pm
        TriggerEvaluator.onWifiState(context, isConnected = true, ssid = "Home")
        assertEquals(0, executionCount())

        // Alarm moments read Wi-Fi live from the system; simulate being on Home / Office Wi-Fi
        val onHome: (DeviceFacts) -> DeviceFacts = { it.copy(wifiConnected = true, wifiSsid = "Home") }
        val onOffice: (DeviceFacts) -> DeviceFacts = { it.copy(wifiConnected = true, wifiSsid = "Office") }

        TriggerEvaluator.evaluate(context, ConditionEvent.MomentReached(id), routineId = id, adjustFacts = onHome)
        assertEquals(1, executionCount())

        // Next evening at 9 pm, still at home: runs again
        TriggerEvaluator.evaluate(context, ConditionEvent.MomentReached(id), routineId = id, adjustFacts = onHome)
        assertEquals(2, executionCount())

        // 9 pm somewhere else: doesn't run
        TriggerEvaluator.evaluate(context, ConditionEvent.MomentReached(id), routineId = id, adjustFacts = onOffice)
        assertEquals(2, executionCount())
    }

    private suspend fun insertRoutine(type: TriggerType, config: JSONObject, enabled: Boolean = true): Long =
        insertRoutine(listOf(RoutineTrigger(type = type, configJson = config.toString())), enabled = enabled)

    private suspend fun insertRoutine(
        triggers: List<RoutineTrigger>,
        matchMode: String = Routine.MATCH_ALL,
        enabled: Boolean = true
    ): Long {
        val notification = CustomNotificationConfig(title = "Test", scheduleType = "IMMEDIATE")
        return dao.insertRoutine(
            Routine(
                name = "Test ${triggers.joinToString { it.type.name }}",
                isEnabled = enabled,
                triggers = triggers,
                matchMode = matchMode,
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
