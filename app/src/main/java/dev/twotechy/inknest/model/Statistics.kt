package dev.twotechy.inknest.model

import java.time.LocalDate
import java.time.ZoneId

data class ReadingEvent(
    val startedAt: Long,
    val seconds: Long,
    val bookId: Long,
    val page: Long? = null,
    val totalPages: Long? = null,
)

data class StatisticsData(
    val days: Map<LocalDate, StatisticsAmount> = emptyMap(),
    val generation: String? = null,
    val zone: ZoneId? = null,
    val configured: Boolean = false,
    val updatedAt: Long? = null,
    val sync: ReadingSyncStatus = ReadingSyncStatus(),
)

enum class StatisticsGranularity { Days, Weeks, Months, Years }

enum class StatisticsUnit { Time, Pages }

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
