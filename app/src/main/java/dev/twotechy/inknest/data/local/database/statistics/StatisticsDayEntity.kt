package dev.twotechy.inknest.data.local.database.statistics

import androidx.room3.Entity

@Entity(tableName = "statistics_days", primaryKeys = ["generation", "day"])
internal data class StatisticsDayEntity(
    val generation: String,
    val day: Long,
    val seconds: Long,
    val pages: Long?,
)

// Deleting this token invalidates every batch of an import, including a pending source copy.
