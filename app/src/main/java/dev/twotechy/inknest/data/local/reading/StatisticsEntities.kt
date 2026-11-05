package dev.twotechy.inknest.data.local.reading

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Relation

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

@Entity(tableName = "statistics_sources", primaryKeys = ["source"])
internal data class StatisticsSourceEntity(
    val source: String,
    val generation: String,
    val updatedAt: Long,
    val zone: String,
)

@Entity(tableName = "statistics_days", primaryKeys = ["generation", "day"])
internal data class StatisticsDayEntity(
    val generation: String,
    val day: Long,
    val seconds: Long,
    val pages: Long?,
)

// Deleting this token invalidates every batch of an import, including a pending source copy.
@Entity(tableName = "statistics_imports", primaryKeys = ["id"])
internal data class StatisticsImportEntity(val id: String, val source: String)

internal data class StatisticsSnapshot(
    @Embedded val source: StatisticsSourceEntity,
    @Relation(parentColumns = ["generation"], entityColumns = ["generation"])
    val days: List<StatisticsDayEntity>,
)
