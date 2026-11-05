package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.ReadingEvent
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

internal fun miniStatistics(
    events: List<ReadingEvent>,
    today: LocalDate,
    zone: ZoneId,
): Pair<Int, Int> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    var daily = 0L
    var weekly = 0L
    for (event in events) {
        val day = Instant.ofEpochMilli(event.startedAt).atZone(zone).toLocalDate()
        if (event.seconds <= 0) continue
        if (day == today) daily += event.seconds
        if (day >= monday && day <= today) weekly += event.seconds
    }
    return (daily / 60).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() to
        (weekly / 60).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
