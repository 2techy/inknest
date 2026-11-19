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
import dev.twotechy.inknest.ui.components.ActionButton
import dev.twotechy.inknest.ui.components.ReadingHeatmap
import dev.twotechy.inknest.ui.components.ReadingSyncNotice
import dev.twotechy.inknest.ui.components.RefreshAction
import dev.twotechy.inknest.ui.components.ScreenHeader
import dev.twotechy.inknest.ui.components.ScreenScaffold
import dev.twotechy.inknest.ui.components.StatePanel
import dev.twotechy.inknest.ui.theme.InkLayoutTokens

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
                StatsOptionsMenu(state, actions)
            }
        },
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionGap),
        ) {
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
    Column(verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionGap)) {
        StatsHighlights(overview, options.unit)
        StatsSection {
            StatsTrend(overview, options, actions.period)
        }
        StatsSection {
            Column(verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionContentGap)) {
                Text(
                    stringResource(R.string.stats_activity),
                    style = MaterialTheme.typography.titleMedium,
                )
                ReadingHeatmap(overview.activity, overview.lastDay, options.unit)
            }
        }
        StatsSection {
            StatsTotals(overview.totals, options.unit)
        }
    }
}

@Composable
private fun StatsSection(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionContentGap)) {
        HorizontalDivider()
        content()
    }
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
