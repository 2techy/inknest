package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSyncStatus
import kotlinx.coroutines.flow.Flow

internal data class LibraryFolder(
    val uri: String,
    val itemIds: Set<ReadingItemId>,
    val sync: ReadingSyncStatus,
    val loading: Boolean,
)

internal data class LibraryCatalog(
    val books: List<ReadingItem> = emptyList(),
    val configured: Boolean = false,
    val loading: Boolean = false,
    val sync: ReadingSyncStatus = ReadingSyncStatus(),
    val folders: List<LibraryFolder> = emptyList(),
)
internal interface LibraryRepository {
    val catalog: Flow<LibraryCatalog>
    suspend fun refresh()
}
