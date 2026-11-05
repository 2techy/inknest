package dev.twotechy.inknest.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.LibraryRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
internal class LibraryViewModel(
    private val repository: LibraryRepository,
    private val savedState: SavedStateHandle,
    private val queryDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private var refreshJob: Job? = null
    private val options = combine(
        savedState.getStateFlow("libraryQuery", ""),
        savedState.getStateFlow("librarySearching", false),
        savedState.getStateFlow("libraryFilter", LibraryFilter.All.name),
        savedState.getStateFlow("librarySort", LibrarySort.Recent.name),
        savedState.getStateFlow("libraryView", LibraryViewMode.Grid.name),
    ) { query, searching, filter, sort, view ->
        LibraryOptions(
            if (searching) query else "",
            searching,
            LibraryFilter.entries.find { it.name == filter } ?: LibraryFilter.All,
            LibrarySort.entries.find { it.name == sort } ?: LibrarySort.Recent,
            LibraryViewMode.entries.find { it.name == view } ?: LibraryViewMode.Grid,
        )
    }
    private val catalog = repository.catalog
    val uiState = combine(catalog, options) { catalog, options -> catalog to options }
        .mapLatest { (catalog, options) ->
            withContext(queryDispatcher) {
                LibraryUiState(
                    options = options,
                    books = selectLibraryBooks(catalog.books, options),
                    totalCount = catalog.books.size,
                    loading = catalog.loading && catalog.books.isEmpty(),
                    notConfigured = !catalog.configured && !catalog.loading,
                    error = catalog.sync.error,
                    sync = catalog.sync,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun query(value: String) {
        // An IME can finish composing after the search field has already been closed.
        if (savedState.get<Boolean>("librarySearching") != true) return
        savedState["libraryQuery"] = value
    }
    fun searching(value: Boolean) {
        savedState["librarySearching"] = value
        if (!value) savedState["libraryQuery"] = ""
    }
    fun filter(value: LibraryFilter) {
        savedState["libraryFilter"] = value.name
    }
    fun sort(value: LibrarySort) {
        savedState["librarySort"] = value.name
    }
    fun toggleView() {
        savedState["libraryView"] =
            if (savedState.get<String>("libraryView") == LibraryViewMode.List.name) {
                LibraryViewMode.Grid.name
            } else {
                LibraryViewMode.List.name
            }
    }
    fun clearFilters() {
        searching(false)
        filter(LibraryFilter.All)
    }
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            try {
                repository.refresh()
            } catch (_: IOException) {
                // Repository sync state retains cached data and carries the error.
            }
        }
    }
}
