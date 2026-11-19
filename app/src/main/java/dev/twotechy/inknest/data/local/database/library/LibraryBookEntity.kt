package dev.twotechy.inknest.data.local.database.library

import androidx.room3.Entity

@Entity(tableName = "library_books", primaryKeys = ["sourceId", "folderKey", "path"])
internal data class LibraryBookEntity(
    val sourceId: String,
    val folderKey: String,
    val path: String,
    val title: String,
    val author: String? = null,
    val coverUri: String? = null,
    val fingerprint: String? = null,
    val progress: Int? = null,
    val status: String = "Unknown",
    val description: String? = null,
    val checksum: String? = null,
)
