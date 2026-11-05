package dev.twotechy.inknest.ui.apps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.PinnedApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppsViewMode { Grid, List }

data class AppItemUiModel(
    val componentId: String,
    val label: String,
    val isPinned: Boolean,
    val iconVersion: Long = 0,
    val canMoveEarlier: Boolean = false,
    val canMoveLater: Boolean = false,
)

data class AppsUiState(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val viewMode: AppsViewMode = AppsViewMode.Grid,
    val pinnedApps: List<AppItemUiModel> = emptyList(),
    val allApps: List<AppItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val canDismissError: Boolean = false,
)

internal class AppsViewModel(
    private val installedAppsRepository: InstalledAppsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private data class Catalog(
        val apps: List<AppInfo>,
        val pins: List<PinnedApp>,
        val loading: Boolean,
        val error: String?,
    )
    private data class Display(val query: String, val searching: Boolean, val mode: AppsViewMode)

    private val actionError = MutableStateFlow<String?>(null)
    private var reloadJob: Job? = null
    private val catalog = combine(
        installedAppsRepository.installedApps,
        installedAppsRepository.pinnedApps,
        installedAppsRepository.isLoading,
        installedAppsRepository.error,
    ) { apps, pins, loading, error -> Catalog(apps, pins, loading, error) }
    private val display = combine(
        savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, ""),
        savedStateHandle.getStateFlow(KEY_SEARCH_ACTIVE, false),
        savedStateHandle.getStateFlow(KEY_VIEW_MODE, AppsViewMode.Grid.name),
    ) { query, searching, mode ->
        Display(
            query,
            searching,
            AppsViewMode.entries.find {
                it.name == mode
            } ?: AppsViewMode.Grid,
        )
    }

    val uiState: StateFlow<AppsUiState> = combine(catalog, display, actionError) {
            data,
            preferences,
            error,
        ->
        val positions = data.pins.withIndex().associate { it.value.componentId to it.index }
        fun item(id: String, label: String, version: Long): AppItemUiModel {
            val index = positions[id]
            return AppItemUiModel(
                id,
                label,
                index != null,
                version,
                canMoveEarlier = index != null && index > 0,
                canMoveLater = index != null && index < data.pins.lastIndex,
            )
        }
        val query = preferences.query.trim()
        val apps = data.apps.asSequence().filter {
            !preferences.searching || query.isEmpty() ||
                it.label.contains(query, ignoreCase = true) ||
                it.componentId.contains(query, ignoreCase = true)
        }.map { item(it.componentId, it.label, it.iconVersion) }.toList()
        AppsUiState(
            searchQuery = preferences.query,
            isSearchActive = preferences.searching,
            viewMode = preferences.mode,
            allApps = apps,
            pinnedApps = data.pins.map { item(it.componentId, it.label, it.iconVersion) },
            isLoading = data.loading && data.apps.isEmpty(),
            isRefreshing = data.loading && data.apps.isNotEmpty(),
            error = error ?: data.error,
            canDismissError = error != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    fun onSearchQueryChanged(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }
    fun onSearchActiveChanged(active: Boolean) {
        savedStateHandle[KEY_SEARCH_ACTIVE] = active
        if (!active) savedStateHandle[KEY_SEARCH_QUERY] = ""
    }
    fun toggleViewMode() {
        val current = savedStateHandle.get<String>(KEY_VIEW_MODE)
        savedStateHandle[KEY_VIEW_MODE] = if (current == AppsViewMode.List.name) {
            AppsViewMode.Grid.name
        } else {
            AppsViewMode.List.name
        }
    }
    fun pin(componentId: String) = updatePins { installedAppsRepository.pin(componentId) }
    fun unpin(componentId: String) = updatePins { installedAppsRepository.unpin(componentId) }
    fun movePin(componentId: String, offset: Int) = updatePins {
        installedAppsRepository.movePin(componentId, offset)
    }
    private fun updatePins(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                actionError.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                actionError.value = "Unable to save pinned apps. Please try again."
            }
        }
    }
    fun dismissActionError() {
        actionError.value = null
    }
    fun refresh() {
        if (reloadJob?.isActive == true || uiState.value.isRefreshing) return
        reloadJob = viewModelScope.launch { installedAppsRepository.reload() }
    }
    private companion object {
        const val KEY_SEARCH_QUERY = "appsSearchQuery"
        const val KEY_SEARCH_ACTIVE = "appsSearchActive"
        const val KEY_VIEW_MODE = "appsViewMode"
    }
}
