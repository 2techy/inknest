package dev.twotechy.inknest.ui.settings

internal sealed interface AcknowledgementsUiState {
    data object NotLoaded : AcknowledgementsUiState
    data object Loading : AcknowledgementsUiState
    data class Content(val text: String) : AcknowledgementsUiState
    data object Failed : AcknowledgementsUiState
}
