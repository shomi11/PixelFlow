package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Cohesive tactile feedback patterns implemented via Compose [HapticFeedbackType]
 * to provide crisp, purposeful tactile confirmations for button clicks and toggle actions.
 */
class AppHapticFeedback(private val haptic: HapticFeedback) {

    /**
     * Tactile pattern for standard button presses, icon taps, and chip selections.
     */
    fun buttonClick() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Tactile pattern for switch, checkbox, and state toggle actions.
     */
    fun toggleAction(checked: Boolean? = null) {
        if (checked == true) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /**
     * Prominent tactile confirmation for primary actions: Save Routine, Run Now, Apply.
     */
    fun confirmAction() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Tactile confirmation for destructive or dismissive actions: Delete, Discard, Cancel.
     */
    fun rejectOrDelete() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Tactile tick for fine adjustments (color selection, icon pick, slider step, day pick).
     */
    fun selectionTick() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

/**
 * Remember and retrieve the scoped [AppHapticFeedback] in Compose hierarchy.
 */
@Composable
fun rememberAppHapticFeedback(): AppHapticFeedback {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) { AppHapticFeedback(hapticFeedback) }
}
