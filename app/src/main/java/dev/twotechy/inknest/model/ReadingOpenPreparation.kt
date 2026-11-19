package dev.twotechy.inknest.model

sealed interface ReadingOpenPreparation {
    data class Ready(val prepared: PreparedReadingOpen) : ReadingOpenPreparation
    data class Failed(val error: OpenResult) : ReadingOpenPreparation
}
