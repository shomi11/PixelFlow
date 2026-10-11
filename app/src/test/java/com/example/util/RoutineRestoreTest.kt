package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ActionType
import com.example.data.model.RoutineActionItem
import com.example.service.RoutineStateSnapshotManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoutineRestoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSnapshotSaveAndRetrieve() {
        val routineId = 12345L
        RoutineStateSnapshotManager.clearSnapshot(context, routineId)
        assertFalse(RoutineStateSnapshotManager.hasSnapshot(context, routineId))

        // Pre-routine settings: AOD was ON, Battery Saver was OFF
        val previousSettings = listOf(
            RoutineActionItem(type = ActionType.AOD, enabledState = true),
            RoutineActionItem(type = ActionType.BATTERY_SAVER, enabledState = false)
        )

        val saved = RoutineStateSnapshotManager.saveSnapshotIfNotExists(
            context = context,
            routineId = routineId,
            snapshotActions = previousSettings,
            reason = "Bluetooth Connected"
        )
        assertTrue(saved)
        assertTrue(RoutineStateSnapshotManager.hasSnapshot(context, routineId))

        // Ensure attempting to overwrite an active snapshot preserves original settings
        val modifiedSettings = listOf(
            RoutineActionItem(type = ActionType.AOD, enabledState = false),
            RoutineActionItem(type = ActionType.BATTERY_SAVER, enabledState = true)
        )
        val overwriteAttempt = RoutineStateSnapshotManager.saveSnapshotIfNotExists(
            context = context,
            routineId = routineId,
            snapshotActions = modifiedSettings,
            reason = "Re-evaluation"
        )
        assertFalse(overwriteAttempt)

        val retrieved = RoutineStateSnapshotManager.getSnapshot(context, routineId)
        assertNotNull(retrieved)
        assertEquals(2, retrieved!!.size)
        assertEquals(ActionType.AOD, retrieved[0].type)
        assertTrue(retrieved[0].enabledState) // Still true!
        assertEquals(ActionType.BATTERY_SAVER, retrieved[1].type)
        assertFalse(retrieved[1].enabledState) // Still false!

        // Clear snapshot upon restoration
        RoutineStateSnapshotManager.clearSnapshot(context, routineId)
        assertFalse(RoutineStateSnapshotManager.hasSnapshot(context, routineId))
    }
}
