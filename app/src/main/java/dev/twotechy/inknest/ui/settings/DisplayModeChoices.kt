package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.theme.InkSettingsTokens
import dev.twotechy.inknest.ui.theme.InkSpacing
import dev.twotechy.inknest.ui.theme.inkColorScheme

@Composable
internal fun DisplayModeChoices(
    mode: DisplayMode,
    enabled: Boolean,
    onMode: (DisplayMode) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = InkSpacing.md)) {
        val minWidth = InkSettingsTokens.previewMinCardWidth * LocalDensity.current.fontScale
        val halfWidth = (maxWidth - InkSpacing.md) / 2
        val sideBySide = halfWidth >= minWidth
        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            maxItemsInEachRow = if (sideBySide) DisplayMode.entries.size else 1,
            horizontalArrangement = Arrangement.spacedBy(InkSpacing.md),
            verticalArrangement = Arrangement.spacedBy(InkSpacing.md),
        ) {
            DisplayMode.entries.forEach { choice ->
                val size = if (sideBySide) Modifier.weight(1f) else Modifier.fillMaxWidth()
                Column(
                    size.clip(MaterialTheme.shapes.medium).selectable(
                        selected = mode == choice,
                        enabled = enabled,
                        role = Role.RadioButton,
                        interactionSource = null,
                        indication = null,
                        onClick = { onMode(choice) },
                    ),
                    verticalArrangement = Arrangement.spacedBy(InkSpacing.xs),
                ) {
                    DisplayModeOption(choice, mode == choice)
                }
            }
        }
    }
}

@Composable
private fun DisplayModeOption(mode: DisplayMode, selected: Boolean) {
    val colors = inkColorScheme(mode)
    Box {
        DisplayModePreview(mode, colors, selected)
        if (selected) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(InkSpacing.sm).size(InkSettingsTokens.previewIconSize),
                tint = colors.primary,
            )
        }
    }
    Text(stringResource(mode.titleRes()), style = MaterialTheme.typography.titleSmall)
    Text(
        stringResource(
            when (mode) {
                DisplayMode.Grayscale -> R.string.grayscale_description
                DisplayMode.Color -> R.string.color_description
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun DisplayMode.titleRes(): Int = when (this) {
    DisplayMode.Grayscale -> R.string.grayscale_mode
    DisplayMode.Color -> R.string.color_mode
}
