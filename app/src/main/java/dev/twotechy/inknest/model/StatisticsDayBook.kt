package dev.twotechy.inknest.model

data class StatisticsDayBook(
    val id: Long,
    val title: String?,
    val amount: StatisticsAmount,
    val libraryItemId: ReadingItemId? = null,
)
