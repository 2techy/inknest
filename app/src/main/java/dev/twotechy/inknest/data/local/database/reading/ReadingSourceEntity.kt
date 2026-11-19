package dev.twotechy.inknest.data.local.database.reading

import androidx.room3.ColumnInfo
import androidx.room3.Entity

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
