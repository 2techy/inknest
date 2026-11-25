package dev.twotechy.inknest.data.local.database.library

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import dev.twotechy.inknest.data.local.database.books.BookFileEntity

@Entity(
    tableName = "library_books",
    primaryKeys = ["sourceId", "folderKey", "path"],
    foreignKeys = [
        ForeignKey(
            entity = BookFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
        ),
    ],
    indices = [Index("fileId")],
)
internal data class LibraryBookEntity(
    val sourceId: String,
    val folderKey: String,
    val path: String,
    val fileId: String,
    val progress: Int? = null,
    val status: String = "Unknown",
)
