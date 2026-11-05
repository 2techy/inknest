package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.StatisticsAmount
import dev.twotechy.inknest.model.StatisticsGranularity
import dev.twotechy.inknest.model.StatisticsUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class StatisticsBucket(val date: LocalDate, val amount: StatisticsAmount)
enum class StatisticsPeriod { ThisWeek, ThisMonth, ThisYear, AllTime }

data class StatisticsDashboard(val today: LocalDate, val days: Map<LocalDate, StatisticsAmount>) {
    val todayAmount get() = days[today] ?: StatisticsAmount()

    fun total(period: StatisticsPeriod): StatisticsAmount = totalSince(
        when (period) {
            StatisticsPeriod.ThisWeek -> monday(today)
            StatisticsPeriod.ThisMonth -> today.withDayOfMonth(1)
            StatisticsPeriod.ThisYear -> today.withDayOfYear(1)
            StatisticsPeriod.AllTime -> LocalDate.MIN
        },
    )

    fun average(unit: StatisticsUnit): Double? =
        totalSince(today.minusDays(29)).value(unit)?.toDouble()?.div(30)

    fun streak(unit: StatisticsUnit): Int? {
        var date = today
        if ((days[date] ?: StatisticsAmount()).value(unit) == 0L) date = date.minusDays(1)
        var streak = 0
        while (true) {
            val value = (days[date] ?: StatisticsAmount()).value(unit) ?: return null
            if (value <= 0) return streak
            streak++
            date = date.minusDays(1)
        }
    }

    fun chart(granularity: StatisticsGranularity): List<StatisticsBucket> =
        statisticsChartBuckets(days, today, granularity)

    private fun totalSince(first: LocalDate): StatisticsAmount =
        days.filterKeys { it >= first }.values.fold(StatisticsAmount(), StatisticsAmount::plus)
}

private fun monday(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
