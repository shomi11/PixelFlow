package com.example.util

import com.example.data.model.ActionType
import com.example.data.model.RoutineActionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HotspotActionTest {

    @Test
    fun testHotspotActionItem_Summary() {
        val actionOn = RoutineActionItem(type = ActionType.HOTSPOT, enabledState = true)
        assertEquals("Hotspot: On", actionOn.toSummary())

        val actionOff = RoutineActionItem(type = ActionType.HOTSPOT, enabledState = false)
        assertEquals("Hotspot: Off", actionOff.toSummary())
    }

    @Test
    fun testHotspotActionItem_Serialization() {
        val original = RoutineActionItem(type = ActionType.HOTSPOT, enabledState = true)
        val json = original.toJsonObject()
        val restored = RoutineActionItem.fromJsonObject(json)

        assertEquals(ActionType.HOTSPOT, restored.type)
        assertTrue(restored.enabledState)
    }
}
