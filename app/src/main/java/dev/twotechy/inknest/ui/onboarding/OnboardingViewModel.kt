package dev.twotechy.inknest.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.repository.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.UnconnectedInstalledAppsRepository
import dev.twotechy.inknest.data.repository.UnconnectedSourceSettingsRepository
import dev.twotechy.inknest.data.repository.UserPreferencesRepository
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.ui.settings.SourceConnectionUiState
import dev.twotechy.inknest.ui.source.SourceSetupError
import dev.twotechy.inknest.ui.source.SourceSetupStateHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val content = combine(preferences.appPreferences, sourceSetup.state, operation) {
            prefs,
            source,
            action,
        ->
        OnboardingUiState(
            mode = prefs.displayMode,
            saving = action.saving || source.saving,
            saveError = action.saveError,
            preferenceError = action.preferenceError,
            connection = SourceConnectionUiState(
                source.source,
                source.apps,
                action.saving || source.saving,
            ),
            sourceError = source.error == SourceSetupError.Source,
            folderConflict = source.error == SourceSetupError.FolderConflict,
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
        if (step.value == 0) finish() else savedState["setupStep"] = step.value - 1
    }
    fun finish() = perform(finishing = true) {
        preferences.completeOnboarding()
        savedState["setupFinished"] = true
    }
    fun setDisplayMode(mode: DisplayMode) =
        perform(finishing = false) { preferences.setDisplayMode(mode) }
    fun selectSourceApp(packageName: String) = sourceSetup.selectApp(packageName)
    fun requestFolder(folder: SourceFolder) = sourceSetup.requestFolder(folder)
    fun replaceBookFolder(uri: String) = sourceSetup.replaceBookFolder(uri)
    fun removeBookFolder(uri: String) = sourceSetup.removeBookFolder(uri)
    fun acceptFolder(folder: SourceFolder, uri: String, flags: Int) =
        sourceSetup.acceptFolder(folder, uri, flags)
    fun dismissFolder() = sourceSetup.dismissFolder()

    private fun perform(finishing: Boolean, block: suspend () -> Unit) {
        if (operation.value.saving || sourceSetup.state.value.saving) return
        operation.value = Operation(saving = true)
        viewModelScope.launch {
            try {
                block()
                operation.value = Operation()
            } catch (cancelled: CancellationException) {
                operation.value = Operation()
                throw cancelled
            } catch (_: Exception) {
                operation.value = Operation(saveError = finishing, preferenceError = !finishing)
            }
        }
    }
}
