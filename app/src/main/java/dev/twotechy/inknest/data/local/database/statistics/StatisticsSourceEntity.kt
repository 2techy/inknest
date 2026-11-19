package dev.twotechy.inknest.data.local.database.statistics

import androidx.room3.Entity

@Entity(tableName = "statistics_sources", primaryKeys = ["source"])
internal data class StatisticsSourceEntity(
    val source: String,
    val generation: String,
    val updatedAt: Long,
    val zone: String,
)
