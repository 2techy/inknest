package dev.twotechy.inknest.data

import dev.twotechy.inknest.data.repository.StatisticsDashboard
import dev.twotechy.inknest.data.repository.StatisticsPeriod
import dev.twotechy.inknest.data.repository.buildStatisticsDashboard
import dev.twotechy.inknest.data.repository.miniStatistics
import dev.twotechy.inknest.model.ReadingEvent
import dev.twotechy.inknest.model.StatisticsAmount
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

    @Test fun miniStatsUseTimeStreakWithYesterdayGraceAndIgnoreFutureDays() {
        val today = LocalDate.parse("2026-11-09")
        val days = mapOf(
            today.minusDays(2) to dev.twotechy.inknest.model.StatisticsAmount(1, null),
            today.minusDays(1) to dev.twotechy.inknest.model.StatisticsAmount(120, null),
            today.plusDays(1) to dev.twotechy.inknest.model.StatisticsAmount(600, 10),
        )
        assertEquals(0 to 2, miniStatistics(days, today))
        assertEquals(0 to 0, miniStatistics(days, today.plusDays(3)))
        assertEquals(0 to 0, miniStatistics(emptyMap(), today))
    }

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
        assertEquals(300L, dashboard.days[today.minusDays(1)]!!.seconds)
        assertEquals(60L, dashboard.todayAmount.seconds)
        assertEquals(1 to 2, miniStatistics(events, today, zone))
        assertEquals(960L, dashboard.total(StatisticsPeriod.AllTime).seconds)
    }

    @Test fun timezoneAndDstRebinInstantsWithoutDroppingOrDuplicatingDurations() {
        val events = listOf(
            event("2026-11-01T05:30:00Z", 1800),
            event("2026-11-01T06:30:00Z", 1800),
            event("2026-11-02T00:30:00Z", 60),
        )
        val today = LocalDate.parse("2026-11-02")
        assertEquals(0 to 1, miniStatistics(events, today, ZoneId.of("America/New_York")))
        assertEquals(1 to 2, miniStatistics(events, today, ZoneId.of("UTC")))
        val dashboard = buildStatisticsDashboard(events, today, ZoneId.of("America/New_York"))
        val summary = dashboard.chart(StatisticsPeriod.ThisMonth)
        assertEquals(3660L, summary.sumOf { it.amount.seconds })
        assertEquals(1, dashboard.days.size)
        assertEquals(0L, dashboard.todayAmount.seconds)
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

    @Test fun periodsClipCalendarBoundariesAndExcludeFutureReadings() {
        val today = LocalDate.parse("2026-01-05")
        val stats = StatisticsDashboard(
            today,
            mapOf(
                LocalDate.parse("2025-12-28") to StatisticsAmount(60, 1),
                LocalDate.parse("2025-12-29") to StatisticsAmount(120, 1),
                LocalDate.parse("2026-01-04") to StatisticsAmount(180, 1),
                today to StatisticsAmount(240, 1),
                today.plusDays(1) to StatisticsAmount(900, 1),
            ),
        )
        StatisticsPeriod.entries.forEach { period ->
            val buckets = stats.chart(period)
            val expected = when (period) {
                StatisticsPeriod.ThisWeek -> 240L
                StatisticsPeriod.AllTime -> 600L
                else -> 420L
            }
            assertEquals(expected, buckets.sumOf { it.amount.seconds })
        }
        val weeks = stats.chart(StatisticsPeriod.ThisMonth)
        assertEquals(LocalDate.parse("2026-01-01"), weeks.first().date)
        assertEquals(LocalDate.parse("2026-01-04"), weeks.first().lastDay)
        assertEquals(180L, weeks.first().amount.seconds)
        assertEquals(240L, weeks[1].amount.seconds)
        assertEquals(LocalDate.parse("2026-01-31"), weeks.last().lastDay)
        assertEquals(listOf(2025, 2026), stats.chart(StatisticsPeriod.AllTime).map { it.date.year })
        assertEquals(12, stats.chart(StatisticsPeriod.ThisYear).size)
        assertEquals(7, stats.chart(StatisticsPeriod.ThisWeek).size)
    }

    @Test fun monthWeeksCoverFourToSixWeeksAndKeepLeapDayAndUnknownPages() {
        for ((month, count) in listOf("2021-02-28" to 4, "2024-02-29" to 5, "2026-03-31" to 6)) {
            val today = LocalDate.parse(month)
            val stats = StatisticsDashboard(
                today,
                mapOf(
                    today.withDayOfMonth(1).minusDays(1) to StatisticsAmount(999, 5),
                    today to StatisticsAmount(60, null),
                    today.plusDays(1) to StatisticsAmount(999, 5),
                ),
            )
            val weeks = stats.chart(StatisticsPeriod.ThisMonth)
            assertEquals(count, weeks.size)
            assertEquals(today, weeks.last().lastDay)
            assertEquals(60L, weeks.sumOf { it.amount.seconds })
            assertNull(weeks.last().amount.pages)
            assertEquals(
                (1..today.dayOfMonth).toList(),
                weeks.flatMap { bucket ->
                    (bucket.date.dayOfMonth..bucket.lastDay.dayOfMonth).toList()
                },
            )
        }
    }

    @Test fun emptyHistoryStillHasCurrentYearAndCalendarGapsStayZero() {
        val today = LocalDate.parse("2026-11-05")
        val empty = StatisticsDashboard(today, emptyMap()).chart(StatisticsPeriod.AllTime)
        assertEquals(listOf(2026), empty.map { it.date.year })
        assertEquals(StatisticsAmount(), empty.single().amount)
        val history = StatisticsDashboard(
            today,
            mapOf(
                today.minusYears(2) to StatisticsAmount(60, 1),
                today to StatisticsAmount(120, 2),
                today.plusYears(1) to StatisticsAmount(999, 5),
            ),
        ).chart(StatisticsPeriod.AllTime)
        assertEquals(listOf(60L, 0L, 120L), history.map { it.amount.seconds })
        assertEquals(listOf(1L, 0L, 2L), history.map { it.amount.pages })
    }
}
