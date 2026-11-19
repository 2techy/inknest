package dev.twotechy.inknest.ui.stats

import dev.twotechy.inknest.data.repository.statistics.StatisticsBucket
import dev.twotechy.inknest.data.repository.statistics.StatisticsPeriod
import dev.twotechy.inknest.model.StatisticsAmount
import java.time.LocalDate

internal data class StatsOverview(
    val average: Double?,
    val streak: Int?,
    val today: StatisticsAmount,
    val totals: Map<StatisticsPeriod, StatisticsAmount>,
    val activity: Map<LocalDate, StatisticsAmount>,
    val lastDay: LocalDate,
    val chart: List<StatisticsBucket>,
)
