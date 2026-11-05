package dev.twotechy.inknest.data.source

import android.os.CancellationSignal
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

// Call from IO. Android query cancellation interrupts provider/SQLite work before closing resources.
internal suspend fun <T> cancellableQuery(read: (CancellationSignal) -> T): T =
    suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        try {
            val value = read(signal)
            continuation.resume(value)
        } catch (error: Exception) {
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }

// Keeps cancellation connected while a cursor consumer suspends between bounded batches.
internal suspend fun <T> withCancellableSignal(read: suspend (CancellationSignal) -> T): T =
    coroutineScope {
        val signal = CancellationSignal()
        val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                signal.cancel()
            }
        }
        try {
            read(signal)
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            throw error
        } finally {
            watcher.cancel()
        }
    }
