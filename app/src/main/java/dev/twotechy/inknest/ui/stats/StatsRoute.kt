package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun StatsRoute(
    viewModel: StatsViewModel,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(
        state,
        StatsActions(viewModel::granularity, viewModel::unit, viewModel::refresh, onSettings),
        modifier,
        contentPadding,
    )
}
