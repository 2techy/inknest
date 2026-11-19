package dev.twotechy.inknest.data.local.database.statistics

import androidx.room3.Entity
import androidx.room3.Index

@Entity(
    tableName = "statistics_events",
    primaryKeys = ["generation", "position"],
    indices = [Index(value = ["generation", "day", "bookId", "page", "totalPages"])],
)
internal data class StatisticsEventEntity(
    val generation: String,
    val position: Int,
    val startedAt: Long,
    val seconds: Long,
    val bookId: Long,
    val page: Long?,
    val totalPages: Long?,
    val day: Long,
)
