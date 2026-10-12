package com.vpmapps.pixelflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Custom Expressive Material 3 Switch with high-contrast color states.
 * Turns into a vibrant, illuminated active color (vibrant royal blue) when enabled,
 * and a muted, calm slate gray when disabled. Features an animated checkmark icon
 * inside the thumb for unmistakable state feedback.
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = Color(0xFF2563EB), // Vibrant Electric/Royal Blue
    mutedColor: Color = Color(0xFF64748B)  // Muted Slate Gray
) {
    val haptic = LocalHapticFeedback.current

    Switch(
        checked = checked,
        onCheckedChange = if (onCheckedChange != null) {
            { newChecked ->
                if (newChecked) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
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
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                    tint = activeColor
                )
            }
        } else {
            {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(mutedColor.copy(alpha = 0.5f))
                )
            }
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = activeColor,
            checkedBorderColor = activeColor,
            checkedIconColor = activeColor,
            uncheckedThumbColor = Color(0xFFF1F5F9),
            uncheckedTrackColor = Color(0xFF94A3B8).copy(alpha = 0.35f),
            uncheckedBorderColor = Color(0xFF94A3B8).copy(alpha = 0.45f),
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.6f),
            disabledCheckedTrackColor = activeColor.copy(alpha = 0.35f),
            disabledUncheckedThumbColor = Color(0xFFCBD5E1),
            disabledUncheckedTrackColor = Color(0xFF64748B).copy(alpha = 0.15f)
        )
    )
}
