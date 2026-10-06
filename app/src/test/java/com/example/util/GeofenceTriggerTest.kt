package com.example.util

import com.example.background.GeofenceManager
import com.example.data.model.Routine
import com.example.data.model.RoutineTrigger
import com.example.data.model.TriggerType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GeofenceTriggerTest {

    @Test
    fun testComputeDistanceMeters_KnownCoordinates() {
        // Distance between Empire State Building (40.7484, -73.9857)
        // and Times Square (40.7580, -73.9855) in New York City is ~1.07 km (1000m - 1150m)
        val dist = GeofenceManager.computeDistanceMeters(
            40.7484, -73.9857,
            40.7580, -73.9855
        )

        assertTrue("Distance should be approximately 1070 meters, got $dist", dist in 900f..1200f)
    }

    @Test
    fun testLocationSummary_EnterTransition() {
        val config = JSONObject().apply {
            put("label", "Home")
            put("transition", "ENTER")
            put("radiusMeters", 150)
            put("latitude", 37.7749)
            put("longitude", -122.4194)
        }
        val trigger = RoutineTrigger(
            type = TriggerType.LOCATION,
            configJson = config.toString()
        )
        val routine = Routine(
            name = "Home Automation",
            triggers = listOf(trigger),
            actions = emptyList<com.example.data.model.RoutineActionItem>()
        )

        val summary = routine.getTriggerSummary()
        assertEquals("Arriving at \"Home\" (150m)", summary)
    }

    @Test
    fun testLocationSummary_ExitTransition() {
        val config = JSONObject().apply {
            put("label", "Work Office")
            put("transition", "EXIT")
            put("radiusMeters", 200)
            put("latitude", 40.7128)
            put("longitude", -74.0060)
        }
        val trigger = RoutineTrigger(
            type = TriggerType.LOCATION,
            configJson = config.toString()
        )
        val routine = Routine(
            name = "Leaving Work",
            triggers = listOf(trigger),
            actions = emptyList<com.example.data.model.RoutineActionItem>()
        )

        val summary = routine.getTriggerSummary()
        assertEquals("Leaving \"Work Office\" (200m)", summary)
    }
}
