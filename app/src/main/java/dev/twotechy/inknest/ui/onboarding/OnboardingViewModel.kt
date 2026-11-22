package dev.twotechy.inknest.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.apps.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.apps.impl.UnconnectedInstalledAppsRepository
import dev.twotechy.inknest.data.repository.preferences.UserPreferencesRepository
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.repository.sources.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.sources.impl.UnconnectedSourceSettingsRepository
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.settings.SourceConnectionUiState
import dev.twotechy.inknest.ui.shared.SourceSetupStateHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class OnboardingViewModel(
    private val preferences: UserPreferencesRepository,
    private val savedState: SavedStateHandle,
    sources: SourceSettingsRepository = UnconnectedSourceSettingsRepository(),
    apps: InstalledAppsRepository = UnconnectedInstalledAppsRepository(),
) : ViewModel() {
    private data class Operation(
        val saving: Boolean = false,
        val saveError: Boolean = false,
        val preferenceError: Boolean = false,
    )
    private val operation = MutableStateFlow(Operation())
    private val step = savedState.getStateFlow("setupStep", 0)
    private val finished = savedState.getStateFlow("setupFinished", false)
    private val sourceSetup = SourceSetupStateHolder(sources, apps, savedState, viewModelScope)
    private val content = combine(
        preferences.appPreferences,
        sourceSetup.state,
        operation,
        apps.isLoading,
        apps.error,
    ) {
            prefs,
            source,
            action,
            appLoading,
            appError,
        ->
        val saving = action.saving || source.saving
        OnboardingUiState(
            mode = prefs.displayMode,
            saving = saving,
            saveError = action.saveError,
            preferenceError = action.preferenceError,
            koreaderDetection = when {
                source.apps.isNotEmpty() -> KoreaderDetection.Found
                appLoading -> KoreaderDetection.Checking
                appError != null -> KoreaderDetection.Failed
                else -> KoreaderDetection.NotFound
            },
            connection = SourceConnectionUiState(
                source.source,
                source.apps,
                saving,
            ),
            sourceError = source.error,
            bookFolderSelection = source.bookFolderSelection,
            preferences = prefs,
            pendingFolder = source.pendingFolder,
        )
    }
    val uiState = combine(content, step, finished) { state, step, finished ->
        state.copy(step = step, finished = finished)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    fun restart() {
        savedState["setupStep"] = 0
        savedState["setupFinished"] = false
        operation.value = Operation()
        sourceSetup.dismissError()
        sourceSetup.dismissFolder()
    }
    fun next() {
        savedState["setupStep"] = (step.value + 1).coerceAtMost(2)
    }
    fun back() {
        when (step.value) {
            0 -> finish()

            2 -> {
                discardLauncherCheckpoint()
                savedState["setupStep"] = 1
            }

            else -> savedState["setupStep"] = 0
        }
    }
    fun restoreLauncherStep() {
        savedState["setupStep"] = 2
        savedState["setupFinished"] = false
    }
    suspend fun prepareLauncherRequest(): Boolean = save(finishing = false) {
        preferences.setOnboardingLauncherPending(true)
    }
    fun discardLauncherCheckpoint() {
        viewModelScope.launch {
            try {
                preferences.setOnboardingLauncherPending(false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                operation.update { it.copy(preferenceError = true) }
            }
        }
    }
    fun finish() = perform(finishing = true) {
        preferences.completeOnboarding()
        savedState["setupFinished"] = true
    }
    fun setDisplayMode(mode: DisplayMode) =
        perform(finishing = false) { preferences.setDisplayMode(mode) }
    fun requestFolder(folder: SourceFolder) = sourceSetup.requestFolder(folder)
    fun replaceBookFolder(uri: String) = sourceSetup.replaceBookFolder(uri)
    fun removeBookFolder(uri: String) = sourceSetup.removeBookFolder(uri)
    fun acceptFolder(folder: SourceFolder, uri: String, flags: Int) = sourceSetup.acceptFolder(
        folder,
        uri,
        flags,
        pairKoreader =
            folder == SourceFolder.KOReader && sourceSetup.state.value.apps.isNotEmpty(),
    )
    fun dismissFolder() = sourceSetup.dismissFolder()

    private fun perform(finishing: Boolean, block: suspend () -> Unit) {
        viewModelScope.launch { save(finishing, block) }
    }

    private suspend fun save(finishing: Boolean, block: suspend () -> Unit): Boolean {
        if (operation.value.saving || sourceSetup.state.value.saving) return false
        operation.value = Operation(saving = true)
        return try {
            block()
            operation.value = Operation()
            true
        } catch (cancelled: CancellationException) {
            operation.value = Operation()
            throw cancelled
        } catch (_: Exception) {
            operation.value = Operation(saveError = finishing, preferenceError = !finishing)
            false
        }
    }
}
