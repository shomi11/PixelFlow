package com.vpmapps.pixelflow.background

import com.vpmapps.pixelflow.data.model.Routine
import com.vpmapps.pixelflow.data.model.RoutineTrigger
import com.vpmapps.pixelflow.data.model.TriggerType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

/**
 * Pure decision tests for combined conditions (no database or device needed).
 */
@RunWith(RobolectricTestRunner::class)
class RoutineConditionsTest {

    private val homeWifi = RoutineTrigger(TriggerType.WIFI, JSONObject().put("connected", true).put("ssid", "Home").toString())
    private val charging = RoutineTrigger(TriggerType.BATTERY, JSONObject().put("charging", true).toString())
    private val nightWindow = RoutineTrigger(
        TriggerType.TIME,
        JSONObject().put("startTime", "22:00").put("endTime", "07:00").put("days", "Daily").toString()
    )
    private val ninePm = RoutineTrigger(TriggerType.TIME, JSONObject().put("startTime", "21:00").toString())

    private fun at(dayOfWeek: Int, hour: Int, minute: Int = 0): Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, dayOfWeek)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }

    private fun routine(vararg triggers: RoutineTrigger, matchMode: String = Routine.MATCH_ALL) =
        Routine(id = 7, name = "Test", triggers = triggers.toList(), matchMode = matchMode)

    @Test
    fun allConditions_startTogetherAndEndWhenOneStops() {
        val r = routine(nightWindow, homeWifi)
        val atHomeAtNight = DeviceFacts(wifiConnected = true, wifiSsid = "Home", now = at(Calendar.MONDAY, 22, 30))

        assertEquals(RoutineTransition.START, TriggerEvaluator.decide(r, atHomeAtNight, wasActive = false, momentFired = false))
        assertEquals(RoutineTransition.NONE, TriggerEvaluator.decide(r, atHomeAtNight, wasActive = true, momentFired = false))

        val leftHome = atHomeAtNight.copy(wifiConnected = false, wifiSsid = "")
        assertEquals(RoutineTransition.END, TriggerEvaluator.decide(r, leftHome, wasActive = true, momentFired = false))

        val homeInDaytime = atHomeAtNight.copy(now = at(Calendar.MONDAY, 12))
        assertEquals(RoutineTransition.NONE, TriggerEvaluator.decide(r, homeInDaytime, wasActive = false, momentFired = false))
    }

    @Test
    fun timeOfDayWithState_runsOnlyAtTheMoment() {
        val r = routine(homeWifi, ninePm)
        val atHome = DeviceFacts(wifiConnected = true, wifiSsid = "Home")

        assertTrue(ninePm.isMoment)
        assertEquals(RoutineTransition.NONE, TriggerEvaluator.decide(r, atHome, wasActive = false, momentFired = false))
        assertEquals(RoutineTransition.START, TriggerEvaluator.decide(r, atHome, wasActive = false, momentFired = true))
        assertEquals(RoutineTransition.START, TriggerEvaluator.decide(r, atHome, wasActive = true, momentFired = true))

        val away = atHome.copy(wifiSsid = "Office")
        assertEquals(RoutineTransition.NONE, TriggerEvaluator.decide(r, away, wasActive = false, momentFired = true))
        assertEquals(RoutineTransition.END, TriggerEvaluator.decide(r, away, wasActive = true, momentFired = false))
    }

    @Test
    fun momentOnly_runsOnceWithoutAnOnPeriod() {
        val r = routine(ninePm)
        assertEquals(RoutineTransition.RUN_ONCE, TriggerEvaluator.decide(r, DeviceFacts(), wasActive = false, momentFired = true))
        assertEquals(RoutineTransition.NONE, TriggerEvaluator.decide(r, DeviceFacts(), wasActive = false, momentFired = false))
    }

    @Test
    fun anyCondition_staysOnWhileOneIsMet() {
        val r = routine(charging, homeWifi, matchMode = Routine.MATCH_ANY)
        val chargingOnly = DeviceFacts(isCharging = true)

        assertEquals(RoutineTransition.START, TriggerEvaluator.decide(r, chargingOnly, wasActive = false, momentFired = false))

        val homeOnly = DeviceFacts(wifiConnected = true, wifiSsid = "Home")
        assertEquals(RoutineTransition.NONE, TriggerEvaluator.decide(r, homeOnly, wasActive = true, momentFired = false))
        assertEquals(RoutineTransition.END, TriggerEvaluator.decide(r, DeviceFacts(), wasActive = true, momentFired = false))
    }

    @Test
    fun anyCondition_momentAloneIsEnough() {
        val r = routine(homeWifi, ninePm, matchMode = Routine.MATCH_ANY)
        assertEquals(RoutineTransition.RUN_ONCE, TriggerEvaluator.decide(r, DeviceFacts(), wasActive = false, momentFired = true))
    }

    @Test
    fun overnightWindow_belongsToTheDayItStarted() {
        val fridayNight = RoutineTrigger(
            TriggerType.TIME,
            JSONObject().put("startTime", "22:00").put("endTime", "02:00").put("days", "Fri").toString()
        )
        assertTrue(fridayNight.isWithinTimeWindow(at(Calendar.FRIDAY, 23)))
        assertTrue(fridayNight.isWithinTimeWindow(at(Calendar.SATURDAY, 1)))
        assertFalse(fridayNight.isWithinTimeWindow(at(Calendar.SATURDAY, 23)))
        assertFalse(fridayNight.isWithinTimeWindow(at(Calendar.FRIDAY, 1)))
    }

    @Test
    fun windowEndMinute_isOutsideTheWindow() {
        assertTrue(nightWindow.isWithinTimeWindow(at(Calendar.TUESDAY, 6, 59)))
        assertFalse(nightWindow.isWithinTimeWindow(at(Calendar.TUESDAY, 7, 0)))
        assertTrue(nightWindow.isWithinTimeWindow(at(Calendar.TUESDAY, 22, 0)))
    }

    @Test
    fun bluetoothAndLocationConditions() {
        val buds = RoutineTrigger(TriggerType.BLUETOOTH, JSONObject().put("connected", true).put("device", "Pixel Buds").toString())
        val atHome = RoutineTrigger(TriggerType.LOCATION, JSONObject().put("transition", "ENTER").toString())
        val awayFromHome = RoutineTrigger(TriggerType.LOCATION, JSONObject().put("transition", "EXIT").toString())

        val facts = DeviceFacts(bluetoothDevices = setOf("Pixel Buds Pro|AA:BB"), geofenceInside = mapOf(7L to true))
        assertTrue(TriggerEvaluator.isMet(buds, 7, facts, wasActive = false))
        assertTrue(TriggerEvaluator.isMet(atHome, 7, facts, wasActive = false))
        assertFalse(TriggerEvaluator.isMet(awayFromHome, 7, facts, wasActive = false))
        // Unknown location state never counts as met
        assertFalse(TriggerEvaluator.isMet(atHome, 8, facts, wasActive = false))
    }

    @Test
    fun batteryLevel_hasHysteresisWhileOn() {
        val low = RoutineTrigger(TriggerType.BATTERY, JSONObject().put("charging", false).put("level", 20).toString())
        assertFalse(TriggerEvaluator.isMet(low, 7, DeviceFacts(batteryPct = 22), wasActive = false))
        assertTrue(TriggerEvaluator.isMet(low, 7, DeviceFacts(batteryPct = 22), wasActive = true))
        assertFalse(TriggerEvaluator.isMet(low, 7, DeviceFacts(batteryPct = 15, isCharging = true), wasActive = true))
    }

    @Test
    fun summary_joinsConditions() {
        assertEquals(
            "Connected to \"Home\" and Daily at 9:00 PM",
            routine(homeWifi, ninePm).getTriggerSummary().replace(' ', ' ')
        )
        assertTrue(routine(homeWifi, charging, matchMode = Routine.MATCH_ANY).getTriggerSummary().contains(" or "))
    }
}
