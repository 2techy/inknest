package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.StatisticsGranularity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

internal fun statisticsChartBuckets(
    days: Map<LocalDate, StatisticsAmount>,
    today: LocalDate,
    granularity: StatisticsGranularity,
): List<StatisticsBucket> {
    val first = granularity.firstBucket(today, days.keys.minOrNull())
    val amounts = mutableMapOf<LocalDate, StatisticsAmount>()
    var date = first
    while (date <= today) {
        amounts[date] = StatisticsAmount()
        date = granularity.nextBucket(date)
    }
    for ((day, amount) in days) {
        if (day < first || day > today) continue
        val key = granularity.bucketStart(day)
        amounts[key] = (amounts[key] ?: StatisticsAmount()) + amount
    }
    return amounts.map { StatisticsBucket(it.key, it.value) }
}

private fun StatisticsGranularity.bucketStart(day: LocalDate): LocalDate = when (this) {
    StatisticsGranularity.Days -> day
    StatisticsGranularity.Weeks -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    StatisticsGranularity.Months -> day.withDayOfMonth(1)
    StatisticsGranularity.Years -> day.withDayOfYear(1)
}

private fun StatisticsGranularity.nextBucket(date: LocalDate): LocalDate = when (this) {
    StatisticsGranularity.Days -> date.plusDays(1)
    StatisticsGranularity.Weeks -> date.plusWeeks(1)
    StatisticsGranularity.Months -> date.plusMonths(1)
    StatisticsGranularity.Years -> date.plusYears(1)
}

private fun StatisticsGranularity.firstBucket(
    today: LocalDate,
    firstRecord: LocalDate?,
): LocalDate = when (this) {
    StatisticsGranularity.Days -> today.minusDays(29)
    StatisticsGranularity.Weeks -> bucketStart(today).minusWeeks(11)
    StatisticsGranularity.Months -> today.withDayOfMonth(1).minusMonths(11)
    StatisticsGranularity.Years -> (firstRecord ?: today).withDayOfYear(1)
}
