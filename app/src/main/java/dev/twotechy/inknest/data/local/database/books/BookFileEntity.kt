package dev.twotechy.inknest.data.local.database.books

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "book_files",
    foreignKeys = [
        ForeignKey(entity = BookEntity::class, parentColumns = ["id"], childColumns = ["bookId"]),
    ],
    indices = [Index("bookId"), Index(value = ["sourceId", "checksum"])],
)
internal data class BookFileEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val sourceId: String,
    val path: String,
    val fingerprint: String?,
    val checksum: String? = null,
)
