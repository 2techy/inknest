package dev.twotechy.inknest.model

data class BookDetails(
    val book: ReadingItem? = null,
    val loading: Boolean = true,
    val statistics: BookReadingStatistics = BookReadingStatistics(),
)
