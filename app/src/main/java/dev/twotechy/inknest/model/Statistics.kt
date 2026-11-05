package dev.twotechy.inknest.model

data class ReadingEvent(
    val startedAt: Long,
    val seconds: Long,
    val bookId: Long,
    val page: Long? = null,
    val totalPages: Long? = null,
)

data class StatisticsData(
    val events: List<ReadingEvent> = emptyList(),
    val configured: Boolean = false,
    val updatedAt: Long? = null,
    val sync: ReadingSyncStatus = ReadingSyncStatus(),
)

enum class StatisticsGranularity { Days, Weeks, Months, Years }

enum class StatisticsUnit { Time, Pages }
