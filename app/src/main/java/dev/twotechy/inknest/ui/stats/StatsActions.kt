package dev.twotechy.inknest.ui.stats

import dev.twotechy.inknest.data.repository.statistics.StatisticsPeriod
import dev.twotechy.inknest.model.StatisticsUnit

internal data class StatsActions(
    val period: (StatisticsPeriod) -> Unit,
    val unit: (StatisticsUnit) -> Unit,
    val refresh: () -> Unit,
    val settings: () -> Unit,
)
