package dev.twotechy.inknest.model

data class StatisticsDayDetail(
    val amount: StatisticsAmount? = null,
    val books: List<StatisticsDayBook> = emptyList(),
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val cacheError: Boolean = false,
    val sync: ReadingSyncStatus = ReadingSyncStatus(),
)
