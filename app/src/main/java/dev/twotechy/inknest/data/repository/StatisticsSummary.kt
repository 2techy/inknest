package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.StatisticsAmount
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

internal fun miniStatistics(
    days: Map<LocalDate, StatisticsAmount>,
    today: LocalDate,
): Pair<Int, Int> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val daily = days[today]?.seconds ?: 0
    val weekly = days.entries.sumOf { (day, amount) ->
        if (day >= monday && day <= today) amount.seconds else 0
    }
    return (daily / 60).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() to
        (weekly / 60).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
