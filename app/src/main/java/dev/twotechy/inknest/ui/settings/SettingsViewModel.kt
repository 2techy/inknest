package dev.twotechy.inknest.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.AcknowledgementsRepository
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.repository.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.UnconnectedInstalledAppsRepository
import dev.twotechy.inknest.data.repository.UserPreferencesRepository
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.model.ThirdPartyLicense
import dev.twotechy.inknest.ui.source.SourceSetupError
import dev.twotechy.inknest.ui.source.SourceSetupStateHolder
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    private val preferences: UserPreferencesRepository,
    sources: SourceSettingsRepository,
    private val acknowledgementsRepository: AcknowledgementsRepository,
    savedState: SavedStateHandle = SavedStateHandle(),
    installedApps: InstalledAppsRepository = UnconnectedInstalledAppsRepository(),
) : ViewModel() {
    private val sourceSetup =
        SourceSetupStateHolder(sources, installedApps, savedState, viewModelScope)
    fun requestFolder(folder: SourceFolder) = sourceSetup.requestFolder(folder)
    fun replaceBookFolder(uri: String) = sourceSetup.replaceBookFolder(uri)
    fun acceptFolder(folder: SourceFolder, uri: String, flags: Int) =
        sourceSetup.acceptFolder(folder, uri, flags)
    fun removeBookFolder(uri: String) = sourceSetup.removeBookFolder(uri)
    fun dismissFolder() = sourceSetup.dismissFolder()

    private data class ActionState(val saving: Boolean = false, val error: SettingsError? = null)
    private val action = MutableStateFlow(ActionState())
    private val acknowledgements =
        MutableStateFlow<Map<ThirdPartyLicense, AcknowledgementsUiState>>(emptyMap())
    private val loadedPreferences: StateFlow<AppPreferences?> = preferences.appPreferences
        .map<AppPreferences, AppPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val uiState: StateFlow<SettingsUiState> = combine(
        loadedPreferences,
        sourceSetup.state,
        preferences.readError,
        action,
        acknowledgements,
    ) { prefs, source, readError, operation, notices ->
        SettingsUiState(
            preferences = prefs,
            source = source.source,
            sourceApps = source.apps,
            pendingFolder = source.pendingFolder,
            saving = operation.saving || source.saving,
            error = operation.error ?: when (source.error) {
                SourceSetupError.Source -> SettingsError.Source
                SourceSetupError.FolderConflict -> SettingsError.FolderConflict
                null -> if (readError != null) SettingsError.Read else null
            },
            acknowledgements = notices,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun loadAcknowledgements(license: ThirdPartyLicense) {
        val current = acknowledgements.value[license]
        if (current is AcknowledgementsUiState.Content ||
            current == AcknowledgementsUiState.Loading
        ) {
            return
        }
        acknowledgements.update { it + (license to AcknowledgementsUiState.Loading) }
        viewModelScope.launch {
            val result = try {
                AcknowledgementsUiState.Content(acknowledgementsRepository.read(license))
            } catch (cancelled: CancellationException) {
                acknowledgements.update { it - license }
                throw cancelled
            } catch (_: IOException) {
                AcknowledgementsUiState.Failed
            }
            acknowledgements.update { it + (license to result) }
        }
    }

    fun setDisplayMode(mode: DisplayMode) = save { preferences.setDisplayMode(mode) }
    fun showHeatmap(show: Boolean) = save { preferences.setShowHomeHeatmap(show) }
    fun showStats(show: Boolean) = save { preferences.setShowHomeStats(show) }
    fun showPins(show: Boolean) = save { preferences.setShowHomePinnedApps(show) }
    fun selectSourceApp(packageName: String) = sourceSetup.selectApp(packageName)
    fun disconnect() = sourceSetup.disconnect()
    fun refresh() = sourceSetup.refresh()
    fun clearCache() = sourceSetup.clearCache()
    fun retryRead() = preferences.retryRead()
    fun dismissError() {
        action.value = action.value.copy(error = null)
        sourceSetup.dismissError()
    }

    private fun save(block: suspend () -> Unit) = perform(SettingsError.Save, block)
    private fun perform(error: SettingsError, block: suspend () -> Unit) {
        if (action.value.saving) return
        action.value = ActionState(saving = true)
        viewModelScope.launch {
            try {
                block()
                action.value = ActionState()
            } catch (cancelled: CancellationException) {
                action.value = ActionState()
                throw cancelled
            } catch (_: Exception) {
                action.value = ActionState(error = error)
            }
        }
    }
}
