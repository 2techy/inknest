package dev.twotechy.inknest.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.AcknowledgementsRepository
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.repository.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.UserPreferencesRepository
import dev.twotechy.inknest.model.AppPreferences
import dev.twotechy.inknest.model.DisplayMode
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    private val preferences: UserPreferencesRepository,
    private val sources: SourceSettingsRepository,
    private val acknowledgementsRepository: AcknowledgementsRepository,
) : ViewModel() {
    private data class ActionState(val saving: Boolean = false, val error: SettingsError? = null)
    private val action = MutableStateFlow(ActionState())
    private val acknowledgements =
        MutableStateFlow<AcknowledgementsUiState>(AcknowledgementsUiState.NotLoaded)
    private val loadedPreferences: StateFlow<AppPreferences?> = preferences.appPreferences
        .map<AppPreferences, AppPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val uiState: StateFlow<SettingsUiState> = combine(
        loadedPreferences,
        sources.state,
        preferences.readError,
        action,
        acknowledgements,
    ) { prefs, source, readError, operation, notices ->
        SettingsUiState(
            preferences = prefs,
            source = source,
            saving = operation.saving,
            error = operation.error ?: if (readError != null) SettingsError.Read else null,
            acknowledgements = notices,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun loadAcknowledgements() {
        if (acknowledgements.value is AcknowledgementsUiState.Content ||
            acknowledgements.value == AcknowledgementsUiState.Loading
        ) {
            return
        }
        acknowledgements.value = AcknowledgementsUiState.Loading
        viewModelScope.launch {
            acknowledgements.value = try {
                AcknowledgementsUiState.Content(acknowledgementsRepository.read())
            } catch (cancelled: CancellationException) {
                acknowledgements.value = AcknowledgementsUiState.NotLoaded
                throw cancelled
            } catch (_: IOException) {
                AcknowledgementsUiState.Failed
            }
        }
    }

    fun setDisplayMode(mode: DisplayMode) = save { preferences.setDisplayMode(mode) }
    fun showStats(show: Boolean) = save { preferences.setShowHomeStats(show) }
    fun showPins(show: Boolean) = save { preferences.setShowHomePinnedApps(show) }
    fun connect(folder: SourceFolder) = sourceAction { sources.connect(folder) }
    fun disconnect() = sourceAction { sources.disconnect() }
    fun refresh() = sourceAction { sources.refresh() }
    fun clearCache() = sourceAction { sources.clearCache() }
    fun retryRead() = preferences.retryRead()
    fun dismissError() {
        action.value = action.value.copy(error = null)
    }

    private fun save(block: suspend () -> Unit) = perform(SettingsError.Save, block)
    private fun sourceAction(block: suspend () -> Unit) = perform(SettingsError.Source, block)
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
