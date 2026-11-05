package dev.twotechy.inknest.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.reading.ReadingDatabase
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.repository.StatisticsImporter
import dev.twotechy.inknest.data.source.stats.StatisticsEventReader
import dev.twotechy.inknest.data.source.stats.StatisticsEventSource
import dev.twotechy.inknest.model.ReadingEvent
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StatisticsImportTest {
    @Test fun batchesPublishAtomicallyAndRebinLocallyAcrossDst() = runBlocking {
        withFixture { fixture ->
            fixture.importer.refresh("tree")
            fixture.verifySummaryAndRebin()
            fixture.verifyFailureAndRetry()
        }
    }

    @Test fun cacheClearAndCancellationInvalidatePendingImports() = runBlocking {
        withFixture { fixture ->
            fixture.importer.refresh("tree")
            fixture.verifyClearDuringRead()
            fixture.verifyCancellation()
        }
    }

    private suspend fun withFixture(block: suspend (ImportFixture) -> Unit) = withTimeout(20_000) {
        val fixture = ImportFixture()
        try {
            fixture.select()
            block(fixture)
        } finally {
            fixture.close()
        }
    }
}

private class ImportFixture {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = Files.createTempDirectory(
        context.cacheDir.toPath(),
        "stats-import-",
    ).toFile()
    private val storeJob = SupervisorJob()
    private val store = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(storeJob + Dispatchers.IO),
    ) { File(directory, "preferences.preferences_pb") }
    private val config = ReadingSourceConfigRepository(store)
    private val database = Room.databaseBuilder<ReadingDatabase>(
        context,
        File(directory, "stats.db").path,
    )
        .setDriver(AndroidSQLiteDriver()).build()
    private val dao = database.statisticsDao()
    private var zone = ZoneId.of("UTC")
    private var calls = 0
    private var failAfterBatch = false
    private var retry = false
    private var entered: CompletableDeferred<Unit>? = null
    private var gate: CompletableDeferred<Unit>? = null
    private val source = object : StatisticsEventSource {
        override suspend fun <T> withEvents(
            tree: String,
            consume: suspend (StatisticsEventReader) -> T,
        ): T {
            calls++
            val previous = dao.source(tree)
            val first = StatisticsEventReader { batch ->
                batch(List(500) { event("2026-11-01T05:30:00Z", page = 1) })
                assertEquals(previous, dao.source(tree))
                entered?.complete(Unit)
                gate?.await()
                if (failAfterBatch || retry) throw IOException("Interrupted snapshot")
                batch(
                    listOf(
                        event("2026-11-01T06:30:00Z", page = 1),
                        event("2026-11-01T06:40:00Z", page = 1, layout = 200),
                        event("2026-11-02T00:30:00Z", page = null),
                        event("2026-11-02T01:00:00Z", page = 9).copy(seconds = 0),
                        event("2027-01-01T00:00:00Z", page = 1),
                    ),
                )
            }
            if (!retry) return consume(first)
            try {
                consume(first)
                error("Expected failed attempt")
            } catch (_: IOException) {
                return consume(
                    StatisticsEventReader { batch ->
                        batch(listOf(event("2026-11-01T05:30:00Z", page = 3)))
                    },
                )
            }
        }
    }
    val importer = StatisticsImporter(config, dao, source, Mutex()) { zone }

    suspend fun select() {
        val before = config.config.first()
        config.activateTree(before, config.treeCandidate(before, SourceFolder.KOReader, "tree"))
    }

    suspend fun verifySummaryAndRebin() {
        val snapshot = checkNotNull(dao.observeSnapshot("tree").first())
        assertEquals(3, snapshot.days.size)
        val utcDay = snapshot.days.single { it.day == day("2026-11-01") }
        assertEquals(502L * 60, utcDay.seconds)
        assertEquals(2L, utcDay.pages)
        assertNull(snapshot.days.single { it.day == day("2026-11-02") }.pages)
        assertEquals(500, dao.batch(snapshot.source.generation, -1, 500).size)
        zone = ZoneId.of("America/New_York")
        importer.rebin("tree", zone)
        val rebinned = checkNotNull(dao.observeSnapshot("tree").first())
        assertEquals(1, calls)
        assertEquals(snapshot.source.updatedAt, rebinned.source.updatedAt)
        assertEquals(503L * 60, rebinned.days.single { it.day == day("2026-11-01") }.seconds)
        assertNull(rebinned.days.single { it.day == day("2026-11-01") }.pages)
        assertTrue(dao.batch(snapshot.source.generation, -1, 1).isEmpty())
        assertEquals(snapshot.days.sumOf { it.seconds }, rebinned.days.sumOf { it.seconds })
    }

    suspend fun verifyFailureAndRetry() {
        val previous = dao.source("tree")
        failAfterBatch = true
        try {
            importer.refresh("tree")
            fail("Partial import must fail")
        } catch (_: IOException) {
            assertEquals(previous, dao.source("tree"))
        }
        failAfterBatch = false
        retry = true
        importer.refresh("tree")
        val snapshot = checkNotNull(dao.observeSnapshot("tree").first())
        assertEquals(60L, snapshot.days.single().seconds)
        assertEquals(1L, snapshot.days.single().pages)
    }

    suspend fun verifyClearDuringRead() = coroutineScope {
        entered = CompletableDeferred()
        gate = CompletableDeferred()
        val pending = async { importer.refresh("tree") }
        entered?.await()
        database.readingDao().clearAll()
        gate?.complete(Unit)
        try {
            pending.await()
            fail("Cleared import must not publish")
        } catch (_: CancellationException) {
            assertNull(dao.source("tree"))
        }
        entered = null
        gate = null
    }

    suspend fun verifyCancellation() = coroutineScope {
        importer.refresh("tree")
        val previous = dao.source("tree")
        entered = CompletableDeferred()
        gate = CompletableDeferred()
        val pending = async { importer.refresh("tree") }
        entered?.await()
        pending.cancelAndJoin()
        assertEquals(previous, dao.source("tree"))
        entered = null
        gate = null
    }

    suspend fun close() {
        storeJob.cancelAndJoin()
        database.close()
        directory.deleteRecursively()
    }
}

private fun event(time: String, page: Long?, layout: Long? = 100) =
    ReadingEvent(Instant.parse(time).toEpochMilli(), 60, 1, page, layout)

private fun day(date: String) = LocalDate.parse(date).toEpochDay()
