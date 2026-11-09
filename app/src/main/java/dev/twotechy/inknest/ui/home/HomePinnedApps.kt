package dev.twotechy.inknest.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.twotechy.inknest.R
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.ui.components.AppIcon
import dev.twotechy.inknest.ui.components.quietClick
import dev.twotechy.inknest.ui.theme.InkAppGridTokens
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun HomePinnedApps(
    pins: List<HomePinUiModel>,
    iconLoader: AppIconLoader?,
    onAppClick: (String) -> Unit,
    onOpenApps: () -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = InkAppGridTokens.horizontalGap
        val minimumCellWidth = InkAppGridTokens.minCellWidth * fontScale
        val columns = ((maxWidth + gap) / (minimumCellWidth + gap)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            val row = pins.take(columns)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { pin ->
                    key(pin.componentId) {
                        HomePinItem(
                            pin,
                            iconLoader,
                            onClick = { onAppClick(pin.componentId) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                // Keep a short row aligned with the responsive app grid.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
            if (pins.size > columns) {
                Text(
                    stringResource(R.string.home_all_pins_count, pins.size),
                    Modifier.quietClick(
                        onOpenApps,
                    ).sizeIn(minHeight = InkAppGridTokens.iconFrame).padding(InkSpacing.md),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun HomePinItem(
    pin: HomePinUiModel,
    iconLoader: AppIconLoader?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.quietClick(onClick).padding(
            horizontal = InkAppGridTokens.cellPadding,
            vertical = InkSpacing.sm,
        ),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(InkAppGridTokens.iconLabelGap),
    ) {
        Box(
            Modifier.size(InkAppGridTokens.iconFrame).border(
                InkBorder.default,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(InkRadius.medium),
            ),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                pin.componentId,
                pin.label,
                pin.iconVersion,
                iconLoader,
                Modifier.size(InkAppGridTokens.iconSize),
            )
        }
        Text(
            pin.label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
