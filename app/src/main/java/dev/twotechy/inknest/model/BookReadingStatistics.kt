package dev.twotechy.inknest.model

import java.time.LocalDate

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

const val BOOK_HISTORY_PAGE_SIZE = 5
