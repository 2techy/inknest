package dev.twotechy.inknest.data.repository

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForegroundRefreshCoordinatorTest {
    @Test fun simultaneousRequestsShareWorkAndCallerCancellationDoesNotCancelSources() = runTest {
        val readingDone = CompletableDeferred<Unit>()
        val statsDone = CompletableDeferred<Unit>()
        var readingCalls = 0
        var statsCalls = 0
        val coordinator = coordinator(
            backgroundScope,
            {
                readingCalls++
                readingDone.await()
            },
            {
                statsCalls++
                statsDone.await()
            },
        )
        val callers = List(3) { async { coordinator.refresh() } }
        runCurrent()
        assertEquals(1, readingCalls)
        assertEquals(1, statsCalls)
        callers.first().cancel()
        readingDone.complete(Unit)
        runCurrent()
        assertFalse(callers.last().isCompleted)
        statsDone.complete(Unit)
        callers.drop(1).awaitAll()
        coordinator.refresh()
        assertEquals(2, readingCalls)
        assertEquals(2, statsCalls)
    }

    @Test fun expectedReadingFailureDoesNotCancelStatisticsOrPreventNextForeground() = runTest {
        var calls = 0
        val coordinator = coordinator(
            backgroundScope,
            { throw IOException("source unavailable") },
            { calls++ },
        )
        coordinator.refresh()
        coordinator.refresh()
        assertEquals(2, calls)
    }

    private fun coordinator(
        scope: kotlinx.coroutines.CoroutineScope,
        read: suspend () -> Unit,
        stats: suspend () -> Unit,
    ) = ForegroundRefreshCoordinator(
        object : ReadingRepository {
            override val reading = flowOf(ReadingState.Empty)
            override suspend fun refresh() = read()
        },
        object : ReadingStatsRepository {
            override val stats = flowOf<dev.twotechy.inknest.model.ReadingStats?>(null)
            override suspend fun refresh() = stats()
        },
        scope,
    )
}
