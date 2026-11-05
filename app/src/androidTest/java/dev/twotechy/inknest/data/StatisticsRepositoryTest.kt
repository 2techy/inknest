package dev.twotechy.inknest.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.reading.ReadingDatabase
import dev.twotechy.inknest.data.repository.KoreaderStatisticsRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.model.ReadingEvent
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsRepositoryTest {
    @Test fun snapshotReplacementFailureDedupeAndSourceIsolation() = runBlocking {
        withTimeout(20_000) {
            val fixture = StatisticsFixture()
            try {
                fixture.select("first")
                fixture.repository.data.first { it.updatedAt != null && !it.sync.running }
                fixture.verifyReplacementAndFailure()
                fixture.verifySharedRefresh()
                fixture.verifySourceIsolationAndClear()
            } finally {
                fixture.close()
            }
        }
    }
}

private class StatisticsFixture {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = Files.createTempDirectory(
        context.cacheDir.toPath(),
        "statistics-test-",
    ).toFile()
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database = Room.databaseBuilder<ReadingDatabase>(
        context,
        File(directory, "stats.db").path,
    )
        .setDriver(AndroidSQLiteDriver()).build()
    private val store = PreferenceDataStoreFactory.create(scope = storeScope) {
        File(directory, "source.preferences_pb")
    }
    private val config = ReadingSourceConfigRepository(store)
    private val fail = AtomicBoolean(false)
    private val calls = AtomicInteger()
    private var gate = CompletableDeferred<Unit>().apply { complete(Unit) }
    private val transition = Mutex()
    val repository = KoreaderStatisticsRepository(config, database.statisticsDao(), {
        calls.incrementAndGet()
        gate.await()
        if (fail.get()) throw IOException("Unavailable")
        listOf(ReadingEvent(1000, if (it == "first") 60 else 120, 1))
    }, scope, transition)

    suspend fun select(tree: String) {
        val before = config.config.first()
        config.activateTree(before, config.treeCandidate(before, SourceFolder.KOReader, tree))
    }

    suspend fun verifyReplacementAndFailure() {
        repository.refresh()
        assertEquals(60L, repository.data.first { !it.sync.running }.events.sumOf { it.seconds })
        assertEquals(1, database.statisticsDao().observeSnapshot("first").first()?.events?.size)
        fail.set(true)
        repository.refresh()
        val stale = repository.data.first { it.sync.error != null }
        assertEquals(60L, stale.events.single().seconds)
        assertTrue(stale.sync.stale)
        fail.set(false)
    }

    suspend fun verifySharedRefresh() {
        val before = calls.get()
        gate = CompletableDeferred()
        coroutineScope {
            val callers = List(3) { async { repository.refresh() } }
            repository.data.first { it.sync.running }
            gate.complete(Unit)
            callers.awaitAll()
        }
        assertEquals(before + 1, calls.get())
    }

    suspend fun verifySourceIsolationAndClear() {
        // A source switch while an older snapshot waits to commit cannot overwrite it.
        transition.lock()
        gate = CompletableDeferred()
        val previous = scope.async { repository.refresh() }
        repository.data.first { it.sync.running }
        gate.complete(Unit)
        select("second")
        transition.unlock()
        previous.join()
        val second = repository.data.first {
            it.updatedAt != null && it.events.singleOrNull()?.seconds == 120L
        }
        assertNull(second.sync.error)
        database.readingDao().clearAll()
        repository.data.first { it.updatedAt == null }
        repository.refresh()
        assertEquals(120L, repository.data.first { it.updatedAt != null }.events.single().seconds)
        config.disconnect()
        assertTrue(repository.data.first { !it.configured }.events.isEmpty())
    }

    fun close() {
        scope.cancel()
        storeScope.cancel()
        database.close()
        directory.deleteRecursively()
    }
}
