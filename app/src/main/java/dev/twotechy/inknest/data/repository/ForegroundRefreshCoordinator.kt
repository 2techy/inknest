package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.platform.OperationTracer
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// The application owns refresh work; screen/lifecycle callers only await a shared request.
internal class ForegroundRefreshCoordinator(
    private val reading: ReadingRepository,
    private val statistics: ReadingStatsRepository,
    private val scope: CoroutineScope,
    private val tracer: OperationTracer = OperationTracer(),
) {
    private val requests = Mutex()
    private var request: Deferred<Unit>? = null

    suspend fun refresh() {
        val pending = requests.withLock {
            request?.takeIf { it.isActive }
                ?: scope.async { refreshSources() }.also { request = it }
        }
        pending.await()
    }

    private suspend fun refreshSources() = tracer.begin(OperationTracer.FOREGROUND_REFRESH).use {
        supervisorScope {
            listOf(
                async { refreshSource(reading::refresh) },
                async { refreshSource(statistics::refresh) },
            ).awaitAll()
        }
        Unit
    }

    private suspend fun refreshSource(action: suspend () -> Unit) {
        try {
            action()
        } catch (_: IOException) {
            // Each repository preserves content and exposes the corresponding sync failure.
        }
    }
}
