package dev.twotechy.inknest.ui

import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.InkNestApplication
import dev.twotechy.inknest.MainActivity
import dev.twotechy.inknest.data.repository.ReadingOpenRepository
import dev.twotechy.inknest.model.OpenResult
import dev.twotechy.inknest.model.PreparedReadingOpen
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingOpenPreparation
import dev.twotechy.inknest.model.ReadingOpenState
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.platform.BookIntentLauncher
import dev.twotechy.inknest.platform.ReadingItemDispatcher
import dev.twotechy.inknest.ui.components.ReadingOpenEffect
import dev.twotechy.inknest.ui.components.ReadingOpenErrorActions
import dev.twotechy.inknest.ui.reading.ReadingOpenStateHolder
import dev.twotechy.inknest.ui.theme.InkNestTheme
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingOpenLifecycleTest {
    @Test fun dispatchIsClaimedOnceAndBackgroundingCancelsPreparation() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        (context.applicationContext as InkNestApplication).awaitContainer()
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as MainActivity
        val owner = SupervisorJob()
        val gate = CompletableDeferred<Unit>().apply { complete(Unit) }
        var prepareGate = gate
        val repository = repository { prepareGate }
        val holder =
            ReadingOpenStateHolder(repository, CoroutineScope(owner + Dispatchers.Main.immediate))
        val dispatched = AtomicInteger()
        val dispatcher = ReadingItemDispatcher(
            context,
            repository,
            BookIntentLauncher {
                dispatched.incrementAndGet()
                OpenResult.Dispatched
            },
        )
        try {
            instrumentation.runOnMainSync {
                activity.setContent {
                    val state by holder.state.collectAsStateWithLifecycle()
                    InkNestTheme {
                        ReadingOpenEffect(
                            state,
                            holder.actions,
                            dispatcher,
                            ReadingOpenErrorActions({
                            }, {}),
                        )
                    }
                }
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                holder.actions.open(ReadingItemId("source", "book"))
                holder.actions.open(ReadingItemId("source", "book"))
            }
            await { dispatched.get() == 1 && holder.state.value == ReadingOpenState.Idle }
            assertEquals(ReadingOpenState.Idle, holder.state.value)
            prepareGate = CompletableDeferred()
            instrumentation.runOnMainSync { holder.actions.open(ReadingItemId("source", "book")) }
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            await { !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
            await { holder.state.value == ReadingOpenState.Idle }
            prepareGate.complete(Unit)
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            await { activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
            instrumentation.waitForIdleSync()
            assertEquals(1, dispatched.get())
        } finally {
            prepareGate.complete(Unit)
            owner.cancelAndJoin()
            instrumentation.runOnMainSync { activity.recreate() }
        }
    }

    private fun repository(gate: () -> CompletableDeferred<Unit>) = object : ReadingOpenRepository {
        override suspend fun prepare(item: ReadingItemId): ReadingOpenPreparation {
            gate().await()
            return ReadingOpenPreparation.Ready(
                PreparedReadingOpen(
                    ReadingSourceConfig(sourceId = "source", sourcePackage = "reader"),
                    "content://books/book",
                    "application/epub+zip",
                ),
            )
        }
        override suspend fun isCurrent(prepared: PreparedReadingOpen) = true
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 8_000
        while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50)
        assertTrue("Timed out waiting for lifecycle/opening state", condition())
    }
}
