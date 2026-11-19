package dev.twotechy.inknest.data.repository.library

import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSyncStatus

internal data class LibraryCatalog(
    val books: List<ReadingItem> = emptyList(),
    val configured: Boolean = false,
    val loading: Boolean = false,
    val sync: ReadingSyncStatus = ReadingSyncStatus(),
    val folders: List<LibraryFolder> = emptyList(),
)
