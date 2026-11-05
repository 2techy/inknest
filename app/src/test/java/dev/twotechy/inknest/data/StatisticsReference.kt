package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.ReadingEvent
import dev.twotechy.inknest.model.StatisticsAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

internal fun miniStatistics(
    events: List<ReadingEvent>,
    today: LocalDate,
    zone: ZoneId,
): Pair<Int, Int> = miniStatistics(buildStatisticsDashboard(events, today, zone).days, today)
