package dev.twotechy.inknest.ui.library

import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus

internal enum class LibraryFilter { All, Reading, Finished, Unknown }
internal enum class LibrarySort { Recent, Title, Progress }
internal enum class LibraryViewMode { Grid, List }

internal data class LibraryOptions(
    val query: String = "",
    val searching: Boolean = false,
    val filter: LibraryFilter = LibraryFilter.All,
    val sort: LibrarySort = LibrarySort.Recent,
    val viewMode: LibraryViewMode = LibraryViewMode.Grid,
)

internal data class LibraryUiState(
    val options: LibraryOptions = LibraryOptions(),
    val books: List<ReadingItem> = emptyList(),
    val totalCount: Int = 0,
    val loading: Boolean = true,
    val notConfigured: Boolean = false,
    val error: ReadingSyncIssue? = null,
    val sync: ReadingSyncStatus = ReadingSyncStatus(),
) {
    val isRefreshing: Boolean get() = sync.running && !loading
}

internal data class LibraryActions(
    val query: (String) -> Unit,
    val searching: (Boolean) -> Unit,
    val filter: (LibraryFilter) -> Unit,
    val sort: (LibrarySort) -> Unit,
    val toggleView: () -> Unit,
    val clearFilters: () -> Unit,
    val refresh: () -> Unit,
    val settings: () -> Unit,
    val open: (ReadingItem) -> Unit,
)
