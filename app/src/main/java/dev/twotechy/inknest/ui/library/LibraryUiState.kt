package dev.twotechy.inknest.ui.library

import dev.twotechy.inknest.data.repository.library.LibraryFolder
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus

internal data class LibraryUiState(
    val options: LibraryOptions = LibraryOptions(),
    val books: List<ReadingItem> = emptyList(),
    val authors: List<LibraryAuthor> = emptyList(),
    val totalCount: Int = 0,
    val loading: Boolean = true,
    val notConfigured: Boolean = false,
    val error: ReadingSyncIssue? = null,
    val sync: ReadingSyncStatus = ReadingSyncStatus(),
    val folders: List<LibraryFolder> = emptyList(),
) {
    val authorIndex: Boolean get() = options.browse == LibraryBrowse.Authors &&
        options.authorKey == null
    val isRefreshing: Boolean get() = sync.running && !loading
}
