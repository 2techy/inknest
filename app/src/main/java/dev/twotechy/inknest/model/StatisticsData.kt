package dev.twotechy.inknest.model

import java.time.LocalDate
import java.time.ZoneId

data class StatisticsData(
    val days: Map<LocalDate, StatisticsAmount> = emptyMap(),
    val generation: String? = null,
    val zone: ZoneId? = null,
    val configured: Boolean = false,
    val updatedAt: Long? = null,
    val sync: ReadingSyncStatus = ReadingSyncStatus(),
    val sourceTree: String? = null,
)
