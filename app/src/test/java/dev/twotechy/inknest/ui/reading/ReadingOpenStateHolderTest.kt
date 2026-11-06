package dev.twotechy.inknest.ui.reading

import dev.twotechy.inknest.data.repository.TestReadingOpenRepository
import dev.twotechy.inknest.model.OpenResult
import dev.twotechy.inknest.model.PreparedReadingOpen
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingOpenPreparation
import dev.twotechy.inknest.model.ReadingOpenState
import dev.twotechy.inknest.model.ReadingSourceConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingOpenStateHolderTest {
    private val item = ReadingItemId("source", "book.epub")
    private val prepared = PreparedReadingOpen(
        ReadingSourceConfig(sourceId = "source", sourcePackage = "reader"),
        "content://books/book",
        "application/epub+zip",
    )

    @Test fun requestsArePreparedOnceClaimedOnceAndRetryableAfterDispatchFailure() = runTest {
        var calls = 0
        val repository = TestReadingOpenRepository().apply {
            action = {
                calls++
                ReadingOpenPreparation.Ready(prepared)
            }
        }
        val holder = ReadingOpenStateHolder(repository, backgroundScope)
        holder.actions.open(item)
        holder.actions.open(item)
        runCurrent()
        assertEquals(1, calls)
        val ready = holder.state.value as ReadingOpenState.Ready
        assertEquals(prepared, holder.actions.claim(ready.request.id))
        assertNull(holder.actions.claim(ready.request.id))
        holder.actions.open(item)
        assertEquals(1, calls)
        holder.actions.complete(ready.request.id, OpenResult.SourceChanged)
        assertEquals(ReadingOpenState.Error(OpenResult.SourceChanged), holder.state.value)
        holder.actions.open(item)
        runCurrent()
        val retry = holder.state.value as ReadingOpenState.Ready
        holder.actions.claim(retry.request.id)
        holder.actions.complete(retry.request.id, OpenResult.Dispatched)
        assertEquals(ReadingOpenState.Idle, holder.state.value)
    }

    @Test fun cancelledPreparationCannotPublishOverANewerRequest() = runTest {
        val done = CompletableDeferred<Unit>()
        val repository = TestReadingOpenRepository().apply {
            action =
                {
                    withContext(NonCancellable) {
                        done.await()
                        ReadingOpenPreparation.Ready(prepared)
                    }
                }
        }
        val holder = ReadingOpenStateHolder(repository, backgroundScope)
        holder.actions.open(item)
        runCurrent()
        holder.actions.cancel()
        assertEquals(ReadingOpenState.Idle, holder.state.value)
        repository.action = { ReadingOpenPreparation.Failed(OpenResult.MissingFile) }
        holder.actions.open(item)
        runCurrent()
        done.complete(Unit)
        runCurrent()
        assertEquals(ReadingOpenState.Error(OpenResult.MissingFile), holder.state.value)
    }

    @Test fun pauseDropsReadyRequestAndIgnoresLateDispatchResults() = runTest {
        val repository = TestReadingOpenRepository().apply {
            action = { ReadingOpenPreparation.Ready(prepared) }
        }
        val holder = ReadingOpenStateHolder(repository, backgroundScope)
        holder.actions.open(item)
        runCurrent()
        val ready = holder.state.value as ReadingOpenState.Ready
        holder.actions.cancel()
        assertNull(holder.actions.claim(ready.request.id))
        holder.actions.complete(ready.request.id, OpenResult.Unavailable)
        assertEquals(ReadingOpenState.Idle, holder.state.value)
        assertEquals(
            ReadingOpenState.Idle,
            ReadingOpenStateHolder(repository, backgroundScope).state.value,
        )
    }
}
