package dev.twotechy.inknest.model

sealed interface ReadingOpenState {
    data object Idle : ReadingOpenState
    data object Preparing : ReadingOpenState
    data class Ready(val request: ReadingOpenRequest) : ReadingOpenState
    data class Dispatching(val request: ReadingOpenRequest) : ReadingOpenState
    data class Error(val error: OpenResult) : ReadingOpenState
}
