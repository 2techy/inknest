package dev.twotechy.inknest.data.local.reading

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
)

@Entity(tableName = "library_folders", primaryKeys = ["sourceId", "folderKey"])
internal data class LibraryFolderEntity(
    val sourceId: String,
    val folderKey: String,
    val updatedAt: Long,
    val complete: Boolean = false,
)
