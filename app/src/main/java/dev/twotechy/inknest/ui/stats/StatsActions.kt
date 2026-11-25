package dev.twotechy.inknest.ui.stats

import dev.twotechy.inknest.data.repository.statistics.StatisticsPeriod
import dev.twotechy.inknest.model.StatisticsUnit
import java.time.LocalDate

internal data class StatsActions(
    val period: (StatisticsPeriod) -> Unit,
    val unit: (StatisticsUnit) -> Unit,
    val refresh: () -> Unit,
    val settings: () -> Unit,
    val day: (LocalDate) -> Unit,
)
