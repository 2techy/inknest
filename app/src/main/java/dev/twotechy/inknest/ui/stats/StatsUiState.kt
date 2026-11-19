package dev.twotechy.inknest.ui.stats

import dev.twotechy.inknest.model.StatisticsData

internal data class StatsUiState(
    val options: StatsOptions = StatsOptions(),
    val data: StatisticsData = StatisticsData(),
    val overview: StatsOverview? = null,
) {
    val isRefreshing: Boolean get() = data.sync.running
}
