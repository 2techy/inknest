package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSyncStatus

internal data class LibraryFolder(
    val uri: String,
    val itemIds: Set<ReadingItemId>,
    val sync: ReadingSyncStatus,
    val loading: Boolean,
)
