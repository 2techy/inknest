package dev.twotechy.inknest.data.repository

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObservationRecoveryTest {
    @Test fun failuresBackOffAndManualRetryPreservesTheLastSnapshot() = runTest {
        val input = MutableStateFlow(Result.success("saved"))
        val retry = ObservationRetry()
        val values = mutableListOf<String>()
        var attempts = 0
        var failed = false
        val source = flow {
            attempts++
            input.map { it.getOrThrow() }.collect { emit(it) }
        }
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            source.recoverObservation(retry, { failed = true }, { failed = false })
                .collect { values += it }
        }
        input.value = Result.failure(IOException("unavailable"))
        runCurrent()
        assertTrue(failed)
        assertEquals(listOf("saved"), values)
        val intervals = listOf(1000L, 2000L, 4000L, 8000L, 16000L, 30000L, 30000L)
        for ((index, delay) in intervals.withIndex()) {
            advanceTimeBy(delay - 1)
            runCurrent()
            assertEquals(index + 1, attempts)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(index + 2, attempts)
        }
        input.value = Result.success("new")
        retry.retry()
        runCurrent()
        assertEquals(listOf("saved", "new"), values)
        assertFalse(failed)
        collection.cancel()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(9, attempts)
    }

    @Test fun cancellationAndProgrammingFailuresAreNotConvertedToReadErrors() = runTest {
        for (error in listOf(CancellationException("cancelled"), IllegalStateException("bug"))) {
            var failed = false
            var attempts = 0
            var observed: Throwable? = null
            try {
                flow<String> {
                    attempts++
                    throw error
                }
                    .recoverObservation(ObservationRetry(), { failed = true }).collect()
            } catch (caught: Throwable) {
                observed = caught
            }
            assertEquals(error, observed)
            assertEquals(1, attempts)
            assertFalse(failed)
        }
    }
}
