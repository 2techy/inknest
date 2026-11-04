package dev.twotechy.inknest.data.local.reading

import androidx.room3.Embedded
import androidx.room3.Entity

@Entity(tableName = "reading_items", primaryKeys = ["sourceId", "snapshotKey", "path"])
internal data class ReadingBookEntity(
    val sourceId: String,
    val snapshotKey: String,
    val path: String,
    val title: String,
    val availability: String,
)

@Entity(tableName = "reading_sources", primaryKeys = ["sourceId", "snapshotKey"])
internal data class ReadingSourceEntity(
    val sourceId: String,
    val snapshotKey: String,
    val currentPath: String? = null,
    val lastAttemptAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val error: String? = null,
)

internal data class CachedReading(
    @Embedded val source: ReadingSourceEntity,
    @Embedded(prefix = "book_") val book: ReadingBookEntity?,
)
