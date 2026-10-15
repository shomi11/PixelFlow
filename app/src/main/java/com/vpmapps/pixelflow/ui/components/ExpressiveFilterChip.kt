package com.vpmapps.pixelflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight

/**
 * Material 3 filter chip with haptic feedback. Colors come from the theme; [accentColor] tints the
 * selected state for content-specific accents.
 */
@Composable
fun ExpressiveFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = FilterChipDefaults.shape
) {
    val haptic = LocalHapticFeedback.current
    FilterChip(
        selected = selected,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onClick()
        },
        label = label,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        shape = shape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor.copy(alpha = 0.16f),
            selectedLabelColor = accentColor,
            selectedLeadingIconColor = accentColor,
            selectedTrailingIconColor = accentColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            selectedBorderColor = accentColor
        ),
        modifier = modifier
    )
}
