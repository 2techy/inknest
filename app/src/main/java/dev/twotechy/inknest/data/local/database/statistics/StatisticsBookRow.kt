package dev.twotechy.inknest.data.local.database.statistics

internal data class StatisticsBookRow(
    val generation: String,
    val bookId: Long,
    val checksum: String?,
    val title: String? = null,
)
