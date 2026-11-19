package dev.twotechy.inknest.data.repository.shared

import android.database.sqlite.SQLiteException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen

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
