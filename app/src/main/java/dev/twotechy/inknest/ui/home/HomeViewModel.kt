package dev.twotechy.inknest.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.preview.HomePreviewController
import dev.twotechy.inknest.data.preview.HomePreviewMode
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.ReadingStatsRepository
import dev.twotechy.inknest.model.PinnedApp
import dev.twotechy.inknest.model.ReadingStats
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class HomeViewModel(
    private val readingRepository: ReadingRepository,
    installedAppsRepository: InstalledAppsRepository,
    readingStatsRepository: ReadingStatsRepository,
    private val savedStateHandle: SavedStateHandle,
    private val previewController: HomePreviewController? = null,
) : ViewModel() {
    private data class RefreshState(val running: Boolean = false, val error: String? = null)

    private val refreshState = MutableStateFlow(RefreshState())
    private var refreshJob: Job? = null

    private val restoredPreviewMode = savedStateHandle.get<String>(PREVIEW_MODE_KEY)
        ?.let { name -> HomePreviewMode.entries.find { it.name == name } }
        ?: HomePreviewMode.Content

    init {
        previewController?.select(restoredPreviewMode)
    }

    val previewMode: StateFlow<HomePreviewMode?> = savedStateHandle
        .getStateFlow(PREVIEW_MODE_KEY, restoredPreviewMode.name)
        .map { name ->
            if (previewController == null) {
                null
            } else {
                HomePreviewMode.entries.find { it.name == name }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            if (previewController == null) null else restoredPreviewMode,
        )

    val uiState: StateFlow<HomeUiState> = combine(
        readingRepository.reading,
        installedAppsRepository.pinnedApps,
        readingStatsRepository.stats,
        refreshState,
    ) { reading, apps, stats, refresh ->
        reading.toHomeUiState(apps, stats).let { state ->
            if (state is HomeUiState.Content) {
                state.copy(isRefreshing = refresh.running, refreshError = refresh.error)
            } else if (refresh.error != null) {
                HomeUiState.Error(refresh.error)
            } else {
                state
            }
        }
    }.catch {
        emit(HomeUiState.Error("Reading data is unavailable. Please try again."))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun onPreviewModeChanged(mode: HomePreviewMode) {
        val controller = previewController ?: return
        savedStateHandle[PREVIEW_MODE_KEY] = mode.name
        controller.select(mode)
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshState.value = RefreshState(running = true)
            try {
                readingRepository.refresh()
                refreshState.value = RefreshState()
            } catch (cancelled: CancellationException) {
                refreshState.value = RefreshState()
                throw cancelled
            } catch (_: Exception) {
                refreshState.value =
                    RefreshState(error = "Unable to refresh books. Please try again.")
            }
        }
    }

    private companion object {
        const val PREVIEW_MODE_KEY = "homePreviewMode"
    }
}

// The same mapping can render previews without constructing a ViewModel.
internal fun ReadingState.toHomeUiState(apps: List<PinnedApp>, stats: ReadingStats?): HomeUiState =
    when (this) {
        ReadingState.Loading -> HomeUiState.Loading

        ReadingState.Empty -> HomeUiState.Empty

        is ReadingState.Error -> HomeUiState.Error(message)

        is ReadingState.Content -> HomeUiState.Content(
            current = BookUiModel(
                current.id,
                current.title,
                current.author,
                current.progress,
                current.coverUri,
            ),
            pinnedApps = apps.map(PinnedApp::label),
            stats = stats?.let { HomeReadingStats(it.todayMinutes, it.weekMinutes) },
        )
    }
