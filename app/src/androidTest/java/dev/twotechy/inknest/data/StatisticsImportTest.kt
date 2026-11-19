package dev.twotechy.inknest.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.database.ReadingDatabase
import dev.twotechy.inknest.data.repository.sources.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.repository.statistics.impl.StatisticsImporter
import dev.twotechy.inknest.data.source.statistics.SnapshotFileMetadata
import dev.twotechy.inknest.data.source.statistics.SnapshotFingerprint
import dev.twotechy.inknest.data.source.statistics.StatisticsEventDataSource
import dev.twotechy.inknest.data.source.statistics.StatisticsEventReader
import dev.twotechy.inknest.model.ReadingEvent
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
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
    @Test fun unchangedSnapshotsSkipButWalAndUnknownMetadataRequireImport() = runBlocking {
        withFixture { it.verifyFingerprints() }
    }

    @Test fun recentMetadataMustBeImportedAgainAfterItSettles() = runBlocking {
        withFixture { it.verifyRecentMetadata() }
    }

    @Test fun clearSourceAndTimezoneChangesInvalidateSkip() = runBlocking {
        withFixture { it.verifySkipInvalidation() }
    }

    @Test fun rejectedPublicationAndCancellationNeverEnableSkip() = runBlocking {
        withFixture { it.verifyRejectedPublication() }
    }

    @Test fun skipUsesCopiedFingerprintAndRequiresThePublishedTimezone() = runBlocking {
        withFixture { it.verifyCopiedFingerprintAndZoneRace() }
    }

    @Test fun closeFailureAndLostActivationDoNotRememberFingerprint() = runBlocking {
        withFixture { it.verifyCloseFailureAndActivationRace() }
    }

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
    private var permissionSnapshots = 0
    private var timestamp = 100_000L
    private var fingerprint: SnapshotFingerprint? = null
    private var copiedFingerprint: SnapshotFingerprint? = null
    private var failOnClose = false
    private var failAfterBatch = false
    private var retry = false
    private var entered: CompletableDeferred<Unit>? = null
    private var gate: CompletableDeferred<Unit>? = null
    private val source = object : StatisticsEventDataSource {
        override suspend fun fingerprint(tree: String): SnapshotFingerprint? {
            assertEquals(
                "statistics-permissions-$permissionSnapshots",
                currentCoroutineContext()[CoroutineName]?.name,
            )
            return fingerprint
        }

        override suspend fun <T> withEvents(
            tree: String,
            consume: suspend (StatisticsEventReader) -> T,
        ): T {
            calls++
            assertEquals(
                "statistics-permissions-$permissionSnapshots",
                currentCoroutineContext()[CoroutineName]?.name,
            )
            val first = snapshotReader(tree)
            val snapshot = object : StatisticsEventReader by first {
                override val fingerprint = copiedFingerprint ?: this@ImportFixture.fingerprint
            }
            if (!retry) {
                val result = consume(snapshot)
                if (failOnClose) throw IOException("Snapshot close failed")
                return result
            }
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
    val importer = StatisticsImporter(
        config,
        dao,
        source,
        Mutex(),
        permissionSnapshot = { CoroutineName("statistics-permissions-${++permissionSnapshots}") },
        now = { timestamp },
    ) { zone }

    private suspend fun snapshotReader(tree: String): StatisticsEventReader {
        val previous = dao.source(tree)
        return StatisticsEventReader { batch ->
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
    }

    suspend fun select(tree: String = "tree") {
        val before = config.config.first()
        config.activateTree(before, config.treeCandidate(before, SourceFolder.KOReader, tree))
    }

    suspend fun verifyFingerprints() {
        fingerprint = stableFingerprint()
        importer.refresh("tree")
        val previous = dao.source("tree")
        importer.refresh("tree")
        assertEquals(1, calls)
        assertEquals(previous, dao.source("tree"))
        fingerprint = stableFingerprint().copy(wal = SnapshotFileMetadata.Present(30, 2))
        importer.refresh("tree")
        assertEquals(2, calls)
        fingerprint = stableFingerprint()
        importer.refresh("tree")
        assertEquals(3, calls)
        fingerprint = stableFingerprint().copy(shm = SnapshotFileMetadata.Present(40, 3))
        importer.refresh("tree")
        assertEquals(4, calls)
        fingerprint = stableFingerprint().copy(wal = SnapshotFileMetadata.Unknown)
        repeat(2) { importer.refresh("tree") }
        assertEquals(6, calls)
        fingerprint = stableFingerprint().copy(database = SnapshotFileMetadata.Present(50, 4))
        importer.refresh("tree")
        assertEquals(7, calls)
    }

    suspend fun verifyRecentMetadata() {
        fingerprint =
            stableFingerprint().copy(database = SnapshotFileMetadata.Present(50, timestamp))
        importer.refresh("tree")
        timestamp += 10_000
        // A write in the same coarse mtime bucket may have happened after the first copy.
        importer.refresh("tree")
        assertEquals(2, calls)
        importer.refresh("tree")
        assertEquals(2, calls)
        fingerprint =
            stableFingerprint().copy(database = SnapshotFileMetadata.Present(50, timestamp + 1))
        repeat(2) { importer.refresh("tree") }
        assertEquals(4, calls)
    }

    suspend fun verifySkipInvalidation() {
        fingerprint = stableFingerprint()
        importer.refresh("tree")
        database.readingDao().clearAll()
        importer.refresh("tree")
        assertEquals(2, calls)
        zone = ZoneId.of("America/New_York")
        importer.rebin("tree", zone)
        importer.refresh("tree")
        assertEquals(3, calls)
        select("other")
        importer.refresh("other")
        select()
        importer.refresh("tree")
        assertEquals(5, calls)
    }

    suspend fun verifyRejectedPublication() = coroutineScope {
        fingerprint = stableFingerprint()
        entered = CompletableDeferred()
        gate = CompletableDeferred()
        val pending = async { importer.refresh("tree") }
        entered?.await()
        select("other")
        gate?.complete(Unit)
        pending.await()
        assertNull(dao.source("tree"))
        entered = null
        gate = null
        select()
        importer.refresh("tree")
        assertEquals(2, calls)
        fingerprint = stableFingerprint().copy(wal = SnapshotFileMetadata.Present(20, 2))
        verifyCancellation()
        importer.refresh("tree")
        assertEquals(5, calls)
    }

    suspend fun verifyCopiedFingerprintAndZoneRace() = coroutineScope {
        fingerprint = stableFingerprint()
        copiedFingerprint = stableFingerprint().copy(wal = SnapshotFileMetadata.Present(20, 2))
        importer.refresh("tree")
        fingerprint = copiedFingerprint
        importer.refresh("tree")
        assertEquals(1, calls)
        fingerprint = stableFingerprint()
        copiedFingerprint = null
        entered = CompletableDeferred()
        gate = CompletableDeferred()
        val previous = dao.source("tree")
        val pending = async { importer.refresh("tree") }
        entered?.await()
        zone = ZoneId.of("America/New_York")
        gate?.complete(Unit)
        pending.await()
        assertEquals(previous, dao.source("tree"))
        entered = null
        gate = null
        importer.refresh("tree")
        assertEquals(3, calls)
    }

    suspend fun verifyCloseFailureAndActivationRace() = coroutineScope {
        fingerprint = stableFingerprint()
        failOnClose = true
        try {
            importer.refresh("tree")
            fail("Snapshot must close successfully before publication")
        } catch (_: IOException) {
            assertNull(dao.source("tree"))
        }
        failOnClose = false
        importer.refresh("tree")
        assertEquals(2, calls)
        val previous = checkNotNull(dao.source("tree"))
        fingerprint = stableFingerprint().copy(wal = SnapshotFileMetadata.Present(20, 2))
        entered = CompletableDeferred()
        gate = CompletableDeferred()
        val pending = async { importer.refresh("tree") }
        entered?.await()
        val replacement = previous.copy(generation = "replacement")
        dao.putSource(replacement)
        gate?.complete(Unit)
        pending.await()
        assertEquals(replacement, dao.source("tree"))
        entered = null
        gate = null
        importer.refresh("tree")
        assertEquals(4, calls)
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
        fingerprint = fingerprint?.copy(database = SnapshotFileMetadata.Present(3, 1))
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

private fun stableFingerprint() = SnapshotFingerprint(
    SnapshotFileMetadata.Present(2, 1),
    SnapshotFileMetadata.Absent,
    SnapshotFileMetadata.Absent,
)
