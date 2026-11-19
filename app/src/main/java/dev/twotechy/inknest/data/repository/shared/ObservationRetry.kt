package dev.twotechy.inknest.data.repository.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

internal class ObservationRetry {
    private val wake = MutableStateFlow(0L)
    val generation: Long get() = wake.value
    fun retry() {
        wake.update { it + 1 }
    }
    suspend fun await(generation: Long, attempt: Int) {
        val timeout = (1_000L shl attempt.coerceIn(0, 5)).coerceAtMost(30_000)
        withTimeoutOrNull(timeout) { wake.first { it != generation } }
    }
}

// Retries only local reads. No fallback emission: downstream keeps its last successful snapshot.
