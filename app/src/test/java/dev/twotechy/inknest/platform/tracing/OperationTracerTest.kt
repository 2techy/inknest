package dev.twotechy.inknest.platform.tracing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OperationTracerTest {
    @Test fun overlappingSpansHaveUniqueCookiesAndCloseOnlyOnce() {
        val begins = mutableListOf<Int>()
        val ends = mutableListOf<Int>()
        val tracer = OperationTracer({ _, cookie -> begins += cookie }, { _, cookie ->
            ends +=
                cookie
        })
        val first = tracer.begin("refresh")
        val second = tracer.begin("refresh")
        assertEquals(OperationTraceState(started = 2, active = 2), tracer.snapshot("refresh"))
        second.close()
        first.close()
        first.close()
        assertEquals(listOf(1, 2), begins)
        assertEquals(listOf(2, 1), ends)
        assertEquals(OperationTraceState(started = 2, completed = 2), tracer.snapshot("refresh"))
        assertEquals(OperationTraceState(), tracer.snapshot("scan"))
    }

    @Test fun cancellationClosesTheSpanAfterSuspension() = runTest {
        val tracer = OperationTracer()
        val pending = CompletableDeferred<Unit>()
        val operation = async { tracer.begin("refresh").use { pending.await() } }
        runCurrent()
        assertEquals(1, tracer.snapshot("refresh").active)
        operation.cancel()
        operation.join()
        assertEquals(OperationTraceState(started = 1, completed = 1), tracer.snapshot("refresh"))
    }
}
