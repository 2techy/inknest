package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.ReadingEvent
import dev.twotechy.inknest.model.StatisticsGranularity
import dev.twotechy.inknest.model.StatisticsUnit
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class StatisticsAmount(val seconds: Long = 0, val pages: Long? = 0) {
    fun value(unit: StatisticsUnit): Long? = when (unit) {
        StatisticsUnit.Time -> seconds
        StatisticsUnit.Pages -> pages
    }

    operator fun plus(other: StatisticsAmount) = StatisticsAmount(
        seconds + other.seconds,
        if (pages == null || other.pages == null) null else pages + other.pages,
    )
}

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

internal fun buildStatisticsDashboard(
    events: List<ReadingEvent>,
    today: LocalDate,
    zone: ZoneId,
): StatisticsDashboard {
    val days = mutableMapOf<LocalDate, DayAccumulator>()
    for (event in events) {
        if (event.seconds <= 0) continue
        val day = Instant.ofEpochMilli(event.startedAt).atZone(zone).toLocalDate()
        if (day > today) continue
        days.getOrPut(day) { DayAccumulator() }.add(event)
    }
    return StatisticsDashboard(today, days.mapValues { it.value.amount() })
}

private data class RecordedPage(val book: Long, val page: Long, val layout: Long?)

private class DayAccumulator {
    private var seconds = 0L
    private var missingPages = false
    private val pages = mutableSetOf<RecordedPage>()

    fun add(event: ReadingEvent) {
        seconds += event.seconds
        val page = event.page
        if (page ==
            null
        ) {
            missingPages = true
        } else {
            pages +=
                RecordedPage(event.bookId, page, event.totalPages)
        }
    }

    fun amount() = StatisticsAmount(seconds, if (missingPages) null else pages.size.toLong())
}

private fun monday(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
