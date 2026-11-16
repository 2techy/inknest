package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.StatisticsPeriod
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.components.ChoiceTab
import dev.twotechy.inknest.ui.components.ContextMenu
import dev.twotechy.inknest.ui.components.ContextMenuItem
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.theme.InkBorder
import dev.twotechy.inknest.ui.theme.InkChoiceTokens
import dev.twotechy.inknest.ui.theme.InkHeaderTokens
import dev.twotechy.inknest.ui.theme.InkRadius
import dev.twotechy.inknest.ui.theme.InkStatsTokens

@Composable
internal fun StatsOptionsMenu(state: StatsUiState, actions: StatsActions) {
    var expanded by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { expanded = false }
    Box {
        HeaderIconButton(R.drawable.ic_more, stringResource(R.string.stats_options)) {
            expanded = true
        }
        if (expanded) {
            ContextMenu(
                onDismiss = { expanded = false },
                modifier = Modifier.width(InkStatsTokens.menuWidth),
                alignToEnd = true,
            ) {
                StatisticsUnit.entries.forEach { unit ->
                    val selected = state.options.unit == unit
                    val label = statsUnitLabel(unit)
                    ContextMenuItem(
                        if (selected) {
                            stringResource(R.string.stats_selected_option, label)
                        } else {
                            label
                        },
                        modifier = Modifier.semantics { this.selected = selected },
                    ) {
                        expanded = false
                        actions.unit(unit)
                    }
                }
                HorizontalDivider()
                ContextMenuItem(
                    stringResource(R.string.stats_refresh),
                    enabled = state.data.configured && !state.isRefreshing,
                ) {
                    expanded = false
                    actions.refresh()
                }
            }
        }
    }
}

@Composable
internal fun statsUnitLabel(unit: StatisticsUnit): String = stringResource(
    if (unit == StatisticsUnit.Time) R.string.stats_unit_time else R.string.stats_unit_pages,
)

@Composable
internal fun StatsPeriods(selected: StatisticsPeriod, onPeriod: (StatisticsPeriod) -> Unit) {
    BoxWithConstraints {
        val width =
            maxOf(maxWidth / 4, InkHeaderTokens.touchTarget * LocalDensity.current.fontScale)
        val density = LocalDensity.current
        val visibleHeight = maxOf(
            InkChoiceTokens.minHeight,
            with(density) { MaterialTheme.typography.labelMedium.lineHeight.toDp() } +
                InkChoiceTokens.verticalPadding * 2,
        )
        val outline = MaterialTheme.colorScheme.outlineVariant
        val scroll = rememberScrollState()
        Row(
            Modifier.drawWithContent {
                drawContent()
                val height = visibleHeight.toPx()
                val top = (size.height - height) / 2
                val stroke = InkBorder.thin.toPx()
                repeat(3) { index ->
                    val x = width.toPx() * (index + 1) - scroll.value
                    if (x > 0 && x < size.width) {
                        drawLine(outline, Offset(x, top), Offset(x, top + height), stroke)
                    }
                }
                drawRoundRect(
                    outline,
                    Offset(stroke / 2, top + stroke / 2),
                    Size(size.width - stroke, height - stroke),
                    CornerRadius(InkRadius.medium.toPx()),
                    style = Stroke(stroke),
                )
            }.horizontalScroll(scroll),
        ) {
            StatisticsPeriod.entries.forEach { period ->
                ChoiceTab(
                    stringResource(period.tabLabel()),
                    selected == period,
                    { onPeriod(period) },
                    Modifier.width(width),
                )
            }
        }
    }
}

private fun StatisticsPeriod.tabLabel(): Int = when (this) {
    StatisticsPeriod.ThisWeek -> R.string.stats_period_week
    StatisticsPeriod.ThisMonth -> R.string.stats_period_month
    StatisticsPeriod.ThisYear -> R.string.stats_period_year
    StatisticsPeriod.AllTime -> R.string.stats_period_all
}
