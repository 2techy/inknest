package dev.twotechy.inknest.data.local.reading

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import androidx.room3.Entity

@Entity(tableName = "reading_items", primaryKeys = ["sourceId", "snapshotKey", "path"])
internal data class ReadingBookEntity(
    val sourceId: String,
    val snapshotKey: String,
    val path: String,
    val title: String,
    val availability: String,
    val author: String? = null,
    val progress: Int? = null,
    val coverUri: String? = null,
    @ColumnInfo(defaultValue = "'Unknown'")
    val status: String = "Unknown",
    val lastReadAt: Long? = null,
    val historyOrder: Int? = null,
    val fingerprint: String? = null,
)

@Entity(tableName = "reading_sources", primaryKeys = ["sourceId", "snapshotKey"])
internal data class ReadingSourceEntity(
    val sourceId: String,
    val snapshotKey: String,
    val currentPath: String? = null,
    val lastAttemptAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val error: String? = null,
    @ColumnInfo(defaultValue = "0")
    val skippedHistoryEntries: Int = 0,
)

internal data class CachedReading(
    @Embedded val source: ReadingSourceEntity,
    @Embedded(prefix = "book_") val book: ReadingBookEntity?,
)
