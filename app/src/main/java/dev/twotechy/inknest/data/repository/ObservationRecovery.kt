package dev.twotechy.inknest.data.repository

import android.database.sqlite.SQLiteException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
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
internal fun <T> Flow<T>.recoverObservation(
    retry: ObservationRetry,
    onError: (Throwable) -> Unit = {},
    onSuccess: () -> Unit = {},
): Flow<T> = flow {
    var failures = 0
    emitAll(
        this@recoverObservation.onEach {
            failures = 0
            onSuccess()
        }.retryWhen { error, _ ->
            if (!error.isRecoverableRead()) return@retryWhen false
            val generation = retry.generation
            onError(error)
            retry.await(generation, failures++)
            true
        },
    )
}

private fun Throwable.isRecoverableRead(): Boolean {
    if (this is CancellationException) return false
    return this is IOException || this is SQLiteException
}
