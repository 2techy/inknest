package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.data.repository.SourceSettingsState
import dev.twotechy.inknest.model.AppPreferences

internal enum class SettingsError { Read, Save, Source }

internal data class SettingsUiState(
    val preferences: AppPreferences? = null,
    val source: SourceSettingsState = SourceSettingsState(),
    val saving: Boolean = false,
    val error: SettingsError? = null,
    val acknowledgements: AcknowledgementsUiState = AcknowledgementsUiState.NotLoaded,
)

internal sealed interface AcknowledgementsUiState {
    data object NotLoaded : AcknowledgementsUiState
    data object Loading : AcknowledgementsUiState
    data class Content(val text: String) : AcknowledgementsUiState
    data object Failed : AcknowledgementsUiState
}
