package com.vpmapps.pixelflow.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Material 3 switch with a check icon in the thumb and haptic feedback on toggle.
 * Colors come from the theme; pass [activeColor] only for content-specific accents
 * (e.g. a routine's own color).
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color? = null
) {
    val haptic = LocalHapticFeedback.current
    val defaults = SwitchDefaults.colors()

    Switch(
        checked = checked,
        onCheckedChange = if (onCheckedChange != null) {
            { newChecked ->
                haptic.performHapticFeedback(
                    if (newChecked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                )
                onCheckedChange(newChecked)
            }
        } else null,
        enabled = enabled,
        modifier = modifier,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        } else null,
        colors = if (activeColor == null) defaults else defaults.copy(
            checkedTrackColor = activeColor,
            checkedIconColor = activeColor,
            disabledCheckedTrackColor = activeColor.copy(alpha = 0.38f)
        )
    )
}
