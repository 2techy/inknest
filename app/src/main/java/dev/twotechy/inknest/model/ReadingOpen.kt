package dev.twotechy.inknest.model

enum class OpenResult {
    Dispatched,
    Unsupported,
    SourceChanged,
    MissingApp,
    MissingFile,
    PermissionLost,
    Unavailable,
}

data class PreparedReadingOpen(val source: ReadingSourceConfig, val uri: String, val mime: String)
sealed interface ReadingOpenPreparation {
    data class Ready(val prepared: PreparedReadingOpen) : ReadingOpenPreparation
    data class Failed(val error: OpenResult) : ReadingOpenPreparation
}
data class ReadingOpenRequest(val id: Long, val prepared: PreparedReadingOpen)
sealed interface ReadingOpenState {
    data object Idle : ReadingOpenState
    data object Preparing : ReadingOpenState
    data class Ready(val request: ReadingOpenRequest) : ReadingOpenState
    data class Dispatching(val request: ReadingOpenRequest) : ReadingOpenState
    data class Error(val error: OpenResult) : ReadingOpenState
}
