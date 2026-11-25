package dev.twotechy.inknest.ui.stats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.model.StatisticsDayDetail
import java.time.LocalDate

@Composable
internal fun StatsRoute(
    viewModel: StatsViewModel,
    openDay: (LocalDate) -> Unit,
    openSettings: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(
        state,
        StatsActions(viewModel::period, viewModel::unit, viewModel::refresh, openSettings, openDay),
        contentPadding = contentPadding,
    )
}

@Composable
internal fun StatsDayRoute(
    viewModel: StatsViewModel,
    navigation: StatsDayNavigation,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val day = navigation.day
    var page by rememberSaveable(day.toEpochDay()) { mutableIntStateOf(0) }
    key(day, page) {
        val observation = remember(viewModel, day, page) {
            viewModel.observeDay(day, page)
        }
        val detail by observation.collectAsStateWithLifecycle(StatisticsDayDetail())
        StatsDayScreen(
            StatsDayUiState(day, detail, page),
            StatsDayActions({
                page = it
            }, navigation.back, viewModel::retryDay, navigation.openBook),
            contentPadding = contentPadding,
        )
    }
}
