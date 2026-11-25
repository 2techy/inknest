package dev.twotechy.inknest.data.local.database.reading

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import dev.twotechy.inknest.data.local.database.books.BookFileEntity

@Entity(
    tableName = "reading_items",
    primaryKeys = ["sourceId", "snapshotKey", "path"],
    foreignKeys = [
        ForeignKey(
            entity = BookFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
        ),
    ],
    indices = [Index("fileId")],
)
internal data class ReadingBookEntity(
    val sourceId: String,
    val snapshotKey: String,
    val path: String,
    val fileId: String,
    val availability: String,
    val progress: Int? = null,
    @ColumnInfo(defaultValue = "'Unknown'")
    val status: String = "Unknown",
    val lastReadAt: Long? = null,
    val historyOrder: Int? = null,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
)
