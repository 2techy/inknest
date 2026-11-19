package dev.twotechy.inknest.data.local.database.statistics

internal data class BookStatisticsTotals(
    val seconds: Long,
    val pages: Long?,
    val days: Int,
    val firstDay: Long?,
    val lastDay: Long?,
)
