package dev.twotechy.inknest.ui.stats

import dev.twotechy.inknest.data.repository.statistics.StatisticsPeriod
import dev.twotechy.inknest.model.StatisticsUnit

internal data class StatsOptions(
    val unit: StatisticsUnit = StatisticsUnit.Time,
    val period: StatisticsPeriod = StatisticsPeriod.ThisWeek,
)
