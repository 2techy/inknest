package dev.twotechy.inknest.data.local.database.statistics

import androidx.room3.Entity
import androidx.room3.Index

@Entity(
    tableName = "statistics_books",
    primaryKeys = ["generation", "bookId"],
    indices = [Index(value = ["generation", "checksum"])],
)
internal data class StatisticsBookEntity(
    val generation: String,
    val bookId: Long,
    val checksum: String,
)
