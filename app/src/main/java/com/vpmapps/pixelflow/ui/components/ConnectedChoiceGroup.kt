package com.vpmapps.pixelflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow

/** One option in a [ConnectedChoiceGroup]. */
data class ChoiceOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val testTag: String? = null
)

/**
 * Material 3 Expressive connected button group for picking exactly one option (replaces
 * single-choice chip rows). Buttons share the row width and morph shape when selected.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConnectedChoiceGroup(
    options: List<ChoiceOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = option.value == selected,
                onCheckedChange = {
                    if (option.value != selected) {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onSelect(option.value)
                    }
                },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton }
                    .then(if (option.testTag != null) Modifier.testTag(option.testTag) else Modifier)
            ) {
                if (option.icon != null) {
                    Icon(option.icon, contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize))
                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                }
                Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
