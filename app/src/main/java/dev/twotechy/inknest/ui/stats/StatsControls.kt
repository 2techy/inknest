package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.StatisticsPeriod
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.components.ChoiceTabGroup
import dev.twotechy.inknest.ui.components.ContextMenu
import dev.twotechy.inknest.ui.components.ContextMenuEntry
import dev.twotechy.inknest.ui.components.ContextMenuEntryRow
import dev.twotechy.inknest.ui.components.HeaderIconButton

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
                alignToEnd = true,
            ) {
                StatisticsUnit.entries.forEach { unit ->
                    val selected = state.options.unit == unit
                    val label = statsUnitLabel(unit)
                    ContextMenuEntryRow(
                        ContextMenuEntry(
                            label,
                            selected = selected,
                            leadingIcon = if (unit == StatisticsUnit.Time) {
                                R.drawable.ic_nav_stats_outline
                            } else {
                                R.drawable.ic_stats_book
                            },
                        ),
                    ) {
                        expanded = false
                        actions.unit(unit)
                    }
                }
                HorizontalDivider()
                ContextMenuEntryRow(
                    ContextMenuEntry(
                        stringResource(R.string.stats_refresh),
                        enabled = state.data.configured && !state.isRefreshing,
                        leadingIcon = R.drawable.ic_settings_sync,
                    ),
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
    ChoiceTabGroup(
        StatisticsPeriod.entries.map { stringResource(it.tabLabel()) },
        StatisticsPeriod.entries.indexOf(selected),
        { onPeriod(StatisticsPeriod.entries[it]) },
    )
}

private fun StatisticsPeriod.tabLabel(): Int = when (this) {
    StatisticsPeriod.ThisWeek -> R.string.stats_period_week
    StatisticsPeriod.ThisMonth -> R.string.stats_period_month
    StatisticsPeriod.ThisYear -> R.string.stats_period_year
    StatisticsPeriod.AllTime -> R.string.stats_period_all
}
