package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsUnit
import java.time.LocalDate

internal fun miniStatistics(
    days: Map<LocalDate, StatisticsAmount>,
    today: LocalDate,
): Pair<Int, Int> {
    val dashboard = StatisticsDashboard(today, days)
    val daily = (dashboard.todayAmount.seconds / 60).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return daily to checkNotNull(dashboard.streak(StatisticsUnit.Time))
}
