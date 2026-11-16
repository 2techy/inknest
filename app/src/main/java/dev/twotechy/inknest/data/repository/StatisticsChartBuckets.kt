package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.StatisticsAmount
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

internal fun statisticsChartBuckets(
    days: Map<LocalDate, StatisticsAmount>,
    today: LocalDate,
    period: StatisticsPeriod,
): List<StatisticsBucket> {
    val first = period.firstDay(today, days.keys)
    val last = period.lastDay(today)
    val amounts = mutableMapOf<LocalDate, StatisticsAmount>()
    var date = first
    while (date <= last) {
        amounts[date] = StatisticsAmount()
        date = period.bucketEnd(date).plusDays(1)
    }
    for ((day, amount) in days) {
        if (day < first || day > today) continue
        val key = period.bucketStart(day, first)
        amounts[key] = (amounts[key] ?: StatisticsAmount()) + amount
    }
    return amounts.map { (day, amount) ->
        StatisticsBucket(day, amount, minOf(last, period.bucketEnd(day)))
    }
}

private fun StatisticsPeriod.bucketEnd(date: LocalDate): LocalDate = when (this) {
    StatisticsPeriod.ThisWeek -> date
    StatisticsPeriod.ThisMonth -> monday(date).plusDays(6)
    StatisticsPeriod.ThisYear -> date.withDayOfMonth(date.lengthOfMonth())
    StatisticsPeriod.AllTime -> date.withDayOfYear(date.lengthOfYear())
}

private fun monday(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

private fun StatisticsPeriod.firstDay(today: LocalDate, dates: Set<LocalDate>): LocalDate =
    when (this) {
        StatisticsPeriod.ThisWeek -> monday(today)

        StatisticsPeriod.ThisMonth -> today.withDayOfMonth(1)

        StatisticsPeriod.ThisYear -> today.withDayOfYear(1)

        StatisticsPeriod.AllTime -> {
            (dates.filter { it <= today }.minOrNull() ?: today).withDayOfYear(1)
        }
    }

private fun StatisticsPeriod.lastDay(today: LocalDate): LocalDate = when (this) {
    StatisticsPeriod.ThisWeek -> monday(today).plusDays(6)

    StatisticsPeriod.ThisMonth -> today.withDayOfMonth(today.lengthOfMonth())

    StatisticsPeriod.ThisYear, StatisticsPeriod.AllTime -> {
        today.withDayOfYear(today.lengthOfYear())
    }
}

private fun StatisticsPeriod.bucketStart(day: LocalDate, first: LocalDate): LocalDate =
    when (this) {
        StatisticsPeriod.ThisWeek -> day
        StatisticsPeriod.ThisMonth -> maxOf(first, monday(day))
        StatisticsPeriod.ThisYear -> day.withDayOfMonth(1)
        StatisticsPeriod.AllTime -> day.withDayOfYear(1)
    }
