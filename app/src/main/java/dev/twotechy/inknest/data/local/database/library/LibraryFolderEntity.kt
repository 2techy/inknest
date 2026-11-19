package dev.twotechy.inknest.data.local.database.library

import androidx.room3.Entity

@Entity(tableName = "library_folders", primaryKeys = ["sourceId", "folderKey"])
internal data class LibraryFolderEntity(
    val sourceId: String,
    val folderKey: String,
    val updatedAt: Long,
    val complete: Boolean = false,
)
