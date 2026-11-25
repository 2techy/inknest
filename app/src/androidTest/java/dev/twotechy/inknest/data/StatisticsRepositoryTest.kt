package dev.twotechy.inknest.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.database.ReadingDatabase
import dev.twotechy.inknest.data.local.database.library.LibraryBookRow
import dev.twotechy.inknest.data.local.database.statistics.StatisticsBookRow
import dev.twotechy.inknest.data.local.database.statistics.StatisticsEventEntity
import dev.twotechy.inknest.data.local.database.statistics.StatisticsSourceEntity
import dev.twotechy.inknest.data.repository.sources.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.repository.statistics.impl.KoreaderStatisticsRepository
import dev.twotechy.inknest.data.source.statistics.StatisticsEventDataSource
import dev.twotechy.inknest.data.source.statistics.StatisticsEventReader
import dev.twotechy.inknest.model.ReadingEvent
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.StatisticsAmount
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId
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
    @Test fun dayDetailUsesActiveCacheAndPaginatesBooks() = runBlocking {
        withTimeout(20_000) {
            val fixture = StatisticsFixture()
            try {
                fixture.select("first")
                fixture.verifyDayDetails()
            } finally {
                fixture.close()
            }
        }
    }

    @Test fun snapshotReplacementFailureDedupeAndSourceIsolation() = runBlocking {
        withTimeout(20_000) {
            val fixture = StatisticsFixture()
            try {
                fixture.select("first")
                fixture.verifyForegroundOwnership()
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
    private val source = object : StatisticsEventDataSource {
        override suspend fun <T> withEvents(
            tree: String,
            consume: suspend (StatisticsEventReader) -> T,
        ): T {
            calls.incrementAndGet()
            gate.await()
            if (fail.get()) throw IOException("Unavailable")
            return consume(
                StatisticsEventReader { batch ->
                    batch(listOf(ReadingEvent(1000, if (tree == "first") 60 else 120, 1)))
                },
            )
        }
    }
    val repository by lazy {
        KoreaderStatisticsRepository(config, database.statisticsDao(), source, scope, transition)
    }

    suspend fun verifyForegroundOwnership() {
        repository.data.first { it.configured }
        assertEquals(0, calls.get())
        repository.refresh()
        assertEquals(1, calls.get())
    }

    suspend fun select(tree: String) {
        val before = config.config.first()
        config.activateTree(before, config.treeCandidate(before, SourceFolder.KOReader, tree))
    }

    suspend fun verifyReplacementAndFailure() {
        repository.refresh()
        assertEquals(
            60L,
            repository.data.first {
                !it.sync.running
            }.days.values.sumOf { it.seconds },
        )
        assertEquals(1, database.statisticsDao().observeSnapshot("first").first()?.days?.size)
        fail.set(true)
        repository.refresh()
        val stale = repository.data.first { it.sync.error != null }
        assertEquals(60L, stale.days.values.single().seconds)
        assertTrue(stale.sync.stale)
        val detail = repository.observeDay(stale.days.keys.single(), 0)
            .first { it.amount != null && it.sync.stale }
        assertEquals(StatisticsAmount(60, null), detail.amount)
        assertEquals(1, detail.books.size)
        fail.set(false)
    }

    suspend fun verifyDayDetails() {
        val day = LocalDate.of(2025, 11, 24)
        val libraryBook = seedDayDetails(day)
        val first = repository.observeDay(day, 0).first { it.amount != null }
        assertEquals(StatisticsAmount(180, null), first.amount)
        assertEquals(30, first.books.size)
        assertTrue(first.hasMore)
        assertEquals(listOf(2L, 1L, 3L), first.books.take(3).map { it.id })
        assertEquals(StatisticsAmount(90, null), first.books[0].amount)
        assertEquals("KOReader fallback", first.books[0].title)
        assertEquals(StatisticsAmount(60, 2), first.books[1].amount)
        assertEquals("Cached title", first.books[1].title)
        assertEquals(libraryBook, first.books[1].libraryItemId)
        assertNull(first.books[0].libraryItemId)
        assertEquals("Stats only", first.books[2].title)
        assertNull(first.books[2].libraryItemId)
        assertNull(first.books[3].title)
        assertEquals(
            listOf(31L, 32L),
            repository.observeDay(day, 1).first { it.amount != null }.books.map { it.id },
        )
        val empty = repository.observeDay(day.minusDays(1), 0).first { it.amount != null }
        assertEquals(StatisticsAmount(), empty.amount)
        assertTrue(empty.books.isEmpty())

        val before = config.config.first()
        config.activateTree(
            before,
            config.treeCandidate(before, SourceFolder.Books, "other-folder", "selected"),
        )
        val after = repository.observeDay(day, 0).first { it.amount != null }
        assertNull(after.books[1].libraryItemId)

        select("second")
        repository.data.first { it.sourceTree == "second" && it.generation != null }
        val second = repository.observeDay(day, 0).first { it.amount != null }
        assertEquals(StatisticsAmount(), second.amount)
        assertTrue(second.books.isEmpty())
        database.readingDao().clearAll()
        repository.data.first { it.generation == null && !it.sync.running }
        assertNull(repository.observeDay(day, 0).first { !it.loading }.amount)
    }

    private suspend fun seedDayDetails(day: LocalDate): ReadingItemId {
        val initial = config.config.first()
        config.activateTree(initial, config.treeCandidate(initial, SourceFolder.Books, "selected"))
        val sourceId = checkNotNull(config.config.first().sourceId)
        val dao = database.statisticsDao()
        val generation = "active"
        val rows = listOf(
            Triple(1L, 30L, 2L),
            Triple(1L, 20L, 2L),
            Triple(1L, 10L, 2L),
            Triple(2L, 90L, null),
        ) + (3L..32L).map { Triple(it, 1L, 1L) }
        dao.putEvents(
            rows.mapIndexed { index, (id, seconds, page) ->
                StatisticsEventEntity(
                    generation,
                    index,
                    1_000,
                    seconds,
                    id,
                    page,
                    if (index == 2) 20 else 10,
                    day.toEpochDay(),
                )
            },
        )
        dao.putBooks(
            listOf(
                StatisticsBookRow(generation, 1, "first", "KOReader title"),
                StatisticsBookRow(generation, 2, "second", "KOReader fallback"),
                StatisticsBookRow(generation, 3, null, "Stats only"),
            ),
        )
        database.libraryDao().putBooks(
            listOf(
                LibraryBookRow(sourceId, "selected", "a", "Cached title", checksum = "first"),
                LibraryBookRow("unrelated", "selected", "a", "Other title", checksum = "first"),
                LibraryBookRow(sourceId, "selected", "b", "One title", checksum = "second"),
                LibraryBookRow(sourceId, "selected", "c", "Another title", checksum = "second"),
            ),
        )
        dao.summarize(generation)
        dao.putSource(StatisticsSourceEntity("first", generation, 1, ZoneId.systemDefault().id))
        return ReadingItemId(sourceId, "a")
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
            it.updatedAt != null && it.days.values.singleOrNull()?.seconds == 120L
        }
        assertNull(second.sync.error)
        database.readingDao().clearAll()
        repository.data.first { it.updatedAt == null }
        repository.refresh()
        assertEquals(
            120L,
            repository.data.first {
                it.updatedAt != null
            }.days.values.single().seconds,
        )
        config.disconnect()
        assertTrue(repository.data.first { !it.configured }.days.isEmpty())
    }

    fun close() {
        scope.cancel()
        storeScope.cancel()
        database.close()
        directory.deleteRecursively()
    }
}
