package dev.twotechy.inknest.model

import java.time.LocalDate

data class ReadingItemId(val sourceId: String, val path: String)

data class ReadingItem(
    val id: ReadingItemId,
    val title: String,
    val author: String?,
    val progress: Int?,
    val coverUri: String? = null,
    val availability: BookAvailability = BookAvailability.Available,
    val status: ReadingStatus = ReadingStatus.Unknown,
    val lastReadAt: Long? = null,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val description: String? = null,
)

data class ReadingStats(val todayMinutes: Int?, val streakDays: Int?)

data class PinnedApp(val componentId: String, val label: String, val iconVersion: Long = 0)

enum class ReadingStatus { Reading, Finished, Abandoned, Unknown }

enum class BookStatisticsStatus { Loading, Unavailable, Unlinked, Ready }

const val BOOK_HISTORY_PAGE_SIZE = 5

data class BookReadingDay(val date: LocalDate, val seconds: Long, val pages: Long?)

data class BookReadingStatistics(
    val status: BookStatisticsStatus = BookStatisticsStatus.Loading,
    val seconds: Long = 0,
    val pages: Long? = null,
    val readingDays: Int = 0,
    val firstRead: LocalDate? = null,
    val lastRead: LocalDate? = null,
    val history: List<BookReadingDay> = emptyList(),
    val hasMore: Boolean = false,
    val historyPage: Int = 0,
    val historyLoading: Boolean = status == BookStatisticsStatus.Loading,
    val error: ReadingSyncIssue? = null,
) {
    val canChangeHistoryPage: Boolean
        get() = status == BookStatisticsStatus.Ready && !historyLoading && error == null
    val historyPageCount: Int get() = (readingDays.coerceAtLeast(1) - 1) / BOOK_HISTORY_PAGE_SIZE +
        1
}

data class BookDetails(
    val book: ReadingItem? = null,
    val loading: Boolean = true,
    val statistics: BookReadingStatistics = BookReadingStatistics(),
)
