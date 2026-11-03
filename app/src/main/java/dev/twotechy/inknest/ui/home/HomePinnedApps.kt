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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = InkAppGridTokens.horizontalGap
        val minimumCellWidth = InkAppGridTokens.minCellWidth * fontScale
        val columns = ((maxWidth + gap) / (minimumCellWidth + gap)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            pins.chunked(columns).forEach { row ->
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
                    // Keep the last row aligned with the same columns as the rows above it.
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
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
