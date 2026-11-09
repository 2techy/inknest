package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.StatisticsGranularity
import dev.twotechy.inknest.model.StatisticsUnit
import dev.twotechy.inknest.ui.components.ActionButton
import dev.twotechy.inknest.ui.components.HeaderIconButton
import dev.twotechy.inknest.ui.components.ReadingHeatmap
import dev.twotechy.inknest.ui.components.ReadingSyncNotice
import dev.twotechy.inknest.ui.components.RefreshAction
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.components.StatePanel
import dev.twotechy.inknest.ui.theme.InkSpacing

internal data class StatsActions(
    val granularity: (StatisticsGranularity) -> Unit,
    val unit: (StatisticsUnit) -> Unit,
    val refresh: () -> Unit,
    val settings: () -> Unit,
)

@Composable
internal fun StatsScreen(
    state: StatsUiState,
    actions: StatsActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    ScreenScaffold(
        modifier,
        contentPadding,
        refresh = RefreshAction(
            state.data.configured,
            state.isRefreshing,
            actions.refresh,
        ),
        header = {
            ScreenHeader(stringResource(R.string.stats_title)) {
                HeaderIconButton(
                    R.drawable.ic_settings_sync,
                    stringResource(R.string.refresh_reading),
                ) {
                    if (!state.isRefreshing) actions.refresh()
                }
            }
        },
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(InkSpacing.lg),
        ) {
            StatsUnits(state.options.unit, actions.unit)
            if (state.data.sync.stale && !state.data.sync.running) {
                ReadingSyncNotice(state.data.sync)
            }
            val overview = state.overview
            if (overview == null) {
                StatsEmpty(state.data.configured, actions)
            } else {
                StatsDashboard(overview, state.options, actions)
            }
        }
    }
}

@Composable
private fun StatsDashboard(overview: StatsOverview, options: StatsOptions, actions: StatsActions) {
    StatsHighlights(overview, options.unit)
    HorizontalDivider()
    Text(stringResource(R.string.stats_trend), style = MaterialTheme.typography.titleMedium)
    StatsGranularities(options.granularity, actions.granularity)
    StatsChart(overview.chart, options)
    HorizontalDivider()
    Text(stringResource(R.string.stats_activity), style = MaterialTheme.typography.titleMedium)
    ReadingHeatmap(overview.activity, overview.lastDay, options.unit)
    HorizontalDivider()
    StatsTotals(overview.totals, options.unit)
}

@Composable
private fun StatsEmpty(configured: Boolean, actions: StatsActions) {
    StatePanel(
        stringResource(R.string.stats_no_data),
        stringResource(R.string.stats_no_data_detail),
        stringResource(R.string.reading_source_action),
        actions.settings,
    )
    if (configured) {
        ActionButton(
            stringResource(R.string.refresh_reading),
            onClick = actions.refresh,
        )
    }
}
