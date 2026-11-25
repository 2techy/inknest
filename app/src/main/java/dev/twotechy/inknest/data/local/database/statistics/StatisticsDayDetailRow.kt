package dev.twotechy.inknest.data.local.database.statistics

internal data class StatisticsDayDetailRow(
    val daySeconds: Long,
    val dayPages: Long?,
    val bookId: Long?,
    val bookSeconds: Long?,
    val bookPages: Long?,
    val title: String?,
    val libraryPath: String?,
)
