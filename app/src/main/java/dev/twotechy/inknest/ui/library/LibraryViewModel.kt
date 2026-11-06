package dev.twotechy.inknest.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.LibraryRepository
import dev.twotechy.inknest.data.repository.ReadingOpenRepository
import dev.twotechy.inknest.ui.reading.ReadingOpenStateHolder
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
    openingRepository: ReadingOpenRepository,
    private val queryDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    val bookOpening = ReadingOpenStateHolder(openingRepository, viewModelScope)
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
        .combine(savedState.getStateFlow<String?>("libraryFolder", null)) { options, folder ->
            options.copy(folderUri = folder)
        }
    private val catalog = repository.catalog
    val uiState = combine(catalog, options) { catalog, options -> catalog to options }
        .mapLatest { (catalog, options) ->
            val selected = options.folderUri?.takeIf { uri ->
                catalog.folders.any { it.uri == uri }
            }
            if (selected != options.folderUri &&
                !catalog.loading
            ) {
                savedState["libraryFolder"] = null
            }
            val effective = options.copy(folderUri = selected)
            withContext(queryDispatcher) {
                libraryUiState(catalog, effective)
            }
        }.combine(bookOpening.state) { content, opening ->
            content.copy(opening = opening)
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
    fun folder(uri: String?) {
        savedState["libraryFolder"] = uri
    }
    fun clearFilters() {
        folder(null)
        searching(false)
        filter(LibraryFilter.All)
    }
    fun refresh() {
        if (refreshJob?.isActive == true || uiState.value.isRefreshing) return
        refreshJob = viewModelScope.launch {
            try {
                repository.refresh()
            } catch (_: IOException) {
                // Repository sync state retains cached data and carries the error.
            }
        }
    }
}
