package dev.twotechy.inknest.data

import dev.twotechy.inknest.data.repository.StatisticsPeriod
import dev.twotechy.inknest.data.repository.buildStatisticsDashboard
import dev.twotechy.inknest.data.repository.miniStatistics
import dev.twotechy.inknest.model.ReadingEvent
import dev.twotechy.inknest.model.StatisticsGranularity
import dev.twotechy.inknest.model.StatisticsUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatisticsSummaryTest {
    private fun event(time: String, seconds: Long, book: Long = 1) =
        ReadingEvent(Instant.parse(time).toEpochMilli(), seconds, book)

    @Test fun rangesKeepRereadsAndAssignCrossMidnightToStartDay() {
        val today = LocalDate.parse("2026-01-01")
        val zone = ZoneId.of("UTC")
        val events = listOf(
            event("2025-12-31T23:59:00Z", 180),
            event("2025-12-31T23:59:00Z", 120),
            event("2026-01-01T12:00:00Z", 60, 2),
            event("2025-01-01T12:00:00Z", 600, 3),
            event("2026-01-02T12:00:00Z", 600),
        )
        val dashboard = buildStatisticsDashboard(events, today, zone)
        val week = dashboard.chart(StatisticsGranularity.Days)
        assertEquals(360L, week.sumOf { it.amount.seconds })
        assertEquals(2, dashboard.days.filterKeys { it >= today.minusDays(6) }.size)
        assertEquals(30, week.size)
        assertEquals(300L, week[28].amount.seconds)
        assertEquals(1 to 6, miniStatistics(events, today, zone))
        val year = dashboard.chart(StatisticsGranularity.Months)
        assertEquals(12, year.size)
        assertEquals(LocalDate.parse("2025-02-01"), year.first().date)
        assertEquals(360L, year.sumOf { it.amount.seconds })
        assertEquals(960L, dashboard.total(StatisticsPeriod.AllTime).seconds)
        assertEquals(
            30,
            dashboard.chart(StatisticsGranularity.Days).size,
        )
    }

    @Test fun timezoneAndDstRebinInstantsWithoutDroppingOrDuplicatingDurations() {
        val events = listOf(
            event("2026-11-01T05:30:00Z", 1800),
            event("2026-11-01T06:30:00Z", 1800),
            event("2026-11-02T00:30:00Z", 60),
        )
        val today = LocalDate.parse("2026-11-02")
        assertEquals(0 to 0, miniStatistics(events, today, ZoneId.of("America/New_York")))
        assertEquals(1 to 1, miniStatistics(events, today, ZoneId.of("UTC")))
        val dashboard = buildStatisticsDashboard(events, today, ZoneId.of("America/New_York"))
        val summary = dashboard.chart(StatisticsGranularity.Days)
        assertEquals(3660L, summary.sumOf { it.amount.seconds })
        assertEquals(1, dashboard.days.size)
        assertEquals(0L, summary.last().amount.seconds)
    }

    @Test fun pagesDeduplicateWithinDayAndLayoutWhileEveryDurationIsRetained() {
        val today = LocalDate.parse("2026-11-05")
        val rows = listOf(
            event("2026-11-04T10:00:00Z", 60).copy(page = 1, totalPages = 100),
            event("2026-11-04T11:00:00Z", 120).copy(page = 1, totalPages = 100),
            event("2026-11-04T11:10:00Z", 30, 2).copy(page = 1, totalPages = 100),
            event("2026-11-04T11:20:00Z", 30).copy(page = 1, totalPages = 200),
            event("2026-11-05T10:00:00Z", 60).copy(page = 1, totalPages = 100),
            event("2026-11-05T11:00:00Z", 0).copy(page = 2, totalPages = 100),
            event("2026-11-06T10:00:00Z", 60).copy(page = 1, totalPages = 100),
        )
        val stats = buildStatisticsDashboard(rows, today, ZoneId.of("UTC"))
        assertEquals(300L, stats.total(StatisticsPeriod.ThisWeek).seconds)
        assertEquals(4L, stats.total(StatisticsPeriod.ThisWeek).pages)
        assertEquals(1L, stats.todayAmount.pages)
        assertEquals(2, stats.streak(StatisticsUnit.Pages))
        assertEquals(4.0 / 30, stats.average(StatisticsUnit.Pages)!!, 0.0001)
        assertEquals(4L, stats.chart(StatisticsGranularity.Days).sumOf { it.amount.pages ?: 0 })
        assertEquals(4L, stats.chart(StatisticsGranularity.Months).sumOf { it.amount.pages ?: 0 })
    }

    @Test fun streakGracePeriodAndUnknownPageDataAreUnitSpecific() {
        val today = LocalDate.parse("2026-11-05")
        val rows = listOf(
            event("2026-11-03T10:00:00Z", 60).copy(page = 1),
            event("2026-11-04T10:00:00Z", 60),
        )
        val stats = buildStatisticsDashboard(rows, today, ZoneId.of("UTC"))
        assertEquals(2, stats.streak(StatisticsUnit.Time))
        assertNull(stats.streak(StatisticsUnit.Pages))
        assertEquals(0L, stats.todayAmount.pages)
        assertNull(stats.total(StatisticsPeriod.ThisMonth).pages)
        assertEquals(120L, stats.total(StatisticsPeriod.AllTime).seconds)
        assertNull(stats.average(StatisticsUnit.Pages))
        assertEquals(
            0,
            buildStatisticsDashboard(
                emptyList(),
                today,
                ZoneId.of("UTC"),
            ).streak(StatisticsUnit.Pages),
        )
    }

    @Test fun calendarWeeksMonthsAndYearsRetainTotalsAcrossYearBoundary() {
        val today = LocalDate.parse("2026-01-05")
        val events = listOf(
            event("2025-12-28T12:00:00Z", 60).copy(page = 1),
            event("2025-12-29T12:00:00Z", 120).copy(page = 1),
            event("2026-01-04T12:00:00Z", 180).copy(page = 1),
            event("2026-01-05T12:00:00Z", 240).copy(page = 1),
            event("2026-01-06T12:00:00Z", 900).copy(page = 1),
        )
        val stats = buildStatisticsDashboard(events, today, ZoneId.of("UTC"))
        StatisticsGranularity.entries.forEach { granularity ->
            val buckets = stats.chart(granularity)
            assertEquals(600L, buckets.sumOf { it.amount.seconds })
            assertEquals(4L, buckets.sumOf { it.amount.pages ?: 0 })
        }
        val weeks = stats.chart(StatisticsGranularity.Weeks)
        assertEquals(12, weeks.size)
        assertEquals(LocalDate.parse("2025-12-29"), weeks[10].date)
        assertEquals(300L, weeks[10].amount.seconds)
        assertEquals(240L, weeks.last().amount.seconds)
        assertEquals(12, stats.chart(StatisticsGranularity.Months).size)
        val years = stats.chart(StatisticsGranularity.Years)
        assertEquals(listOf(2025, 2026), years.map { it.date.year })
        assertEquals(listOf(180L, 420L), years.map { it.amount.seconds })
        assertEquals(
            1,
            buildStatisticsDashboard(emptyList(), today, ZoneId.of("UTC"))
                .chart(StatisticsGranularity.Years).size,
        )
    }
}
