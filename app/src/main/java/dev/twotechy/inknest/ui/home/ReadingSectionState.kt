package dev.twotechy.inknest.ui.home

import dev.twotechy.inknest.model.ReadingSyncIssue

sealed interface ReadingSectionState {
    data object Loading : ReadingSectionState
    data object Empty : ReadingSectionState
    data object NotConfigured : ReadingSectionState
    data class Error(val issue: ReadingSyncIssue) : ReadingSectionState
    data class Content(val current: BookUiModel) : ReadingSectionState
}
