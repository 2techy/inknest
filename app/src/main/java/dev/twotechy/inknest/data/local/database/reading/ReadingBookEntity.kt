package dev.twotechy.inknest.data.local.database.reading

import androidx.room3.ColumnInfo
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
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val description: String? = null,
)
