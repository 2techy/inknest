package dev.twotechy.inknest.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.reading.ReadingCache
import dev.twotechy.inknest.data.local.reading.ReadingDatabase
import dev.twotechy.inknest.data.local.reading.readingCacheKey
import dev.twotechy.inknest.data.repository.DataStoreUserPreferencesRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.RoomReadingRepository
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.source.books.BookMetadata
import dev.twotechy.inknest.data.source.books.FileBookCoverStore
import dev.twotechy.inknest.data.source.koreader.CurrentReadingDataSource
import dev.twotechy.inknest.data.source.koreader.CurrentReadingSnapshot
import dev.twotechy.inknest.data.source.koreader.ReadingRefreshException
import dev.twotechy.inknest.data.source.saf.SourceDocument
import dev.twotechy.inknest.data.source.saf.SourceMetadataFingerprint
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingCacheIntegrationTest {
    @Test fun stableReadingSkipsButMetadataChangesAndRestartRequireRead() = runBlocking {
        withHarness { h ->
            h.enableSkip()
            h.repository.refresh()
            val success = h.repository.sync.first { it.lastSuccessAt != null }.lastSuccessAt
            h.time += 10
            h.repository.refresh()
            assertEquals(1, h.reads)
            assertEquals(2, h.permissionSnapshots)
            assertEquals(
                success,
                h.repository.sync.first {
                    it.lastAttemptAt == h.time
                }.lastSuccessAt,
            )
            h.fingerprint = fingerprint(2)
            h.repository.refresh()
            assertEquals(2, h.reads)
            h.reopen()
            h.repository.refresh()
            assertEquals(3, h.reads)
        }
    }

    @Test fun lostPermissionAndWarningsRecoverWithoutReusingStaleReading() = runBlocking {
        withHarness { h ->
            h.enableSkip()
            h.repository.refresh()
            h.permitted = false
            try {
                h.repository.refresh()
                error("Revoked access must not skip")
            } catch (_: ReadingRefreshException) { }
            h.repository.sync.first { it.error == ReadingSyncIssue.PermissionLost }
            h.permitted = true
            h.repository.refresh()
            assertEquals(3, h.reads)
            h.fingerprint = fingerprint(2)
            h.load = { config ->
                snapshot(config, "A.epub").copy(
                    history = emptyList(),
                    warning = ReadingSyncIssue.InvalidData,
                )
            }
            repeat(2) { h.repository.refresh() }
            assertEquals(5, h.reads)
        }
    }

    @Test fun clearedRowsAndChangedBooksSourceInvalidateRememberedReading() = runBlocking {
        withHarness { h ->
            h.enableSkip()
            h.repository.refresh()
            h.database.readingDao().clearAll()
            h.repository.refresh()
            assertEquals(2, h.reads)
            h.repository.clearCache()
            assertEquals(3, h.reads)
            val before = h.config.config.first()
            h.repository.replaceSource(
                before,
                h.config.treeCandidate(before, SourceFolder.Books, "content://test/new-books"),
            )
            h.repository.refresh()
            assertEquals(5, h.reads)
            h.repository.disconnect()
            assertEquals(
                null,
                h.database.readingDao().get(
                    checkNotNull(before.sourceId),
                    checkNotNull(before.readingCacheKey()).snapshotKey,
                ),
            )
        }
    }

    @Test fun cacheClearWhileFingerprintWaitsCancelsOldSkipDecision() = runBlocking {
        withHarness { h ->
            h.enableSkip()
            h.repository.refresh()
            val entered = CompletableDeferred<Unit>()
            val gate = CompletableDeferred<Unit>()
            h.checkFingerprint = {
                entered.complete(Unit)
                gate.await()
                true
            }
            val pending = async { h.repository.refresh() }
            entered.await()
            h.repository.clearCache()
            try {
                pending.await()
                error("Cleared caller must be cancelled")
            } catch (_: CancellationException) { }
            assertEquals(2, h.reads)
            assertCurrent(h.repository, "A.epub")
        }
    }

    @Test fun failedSaveNeverRemembersFingerprintAndMissingCoverForcesRead() = runBlocking {
        withHarness { h ->
            h.enableSkip()
            h.executeSql(
                "CREATE TRIGGER fail_reading_save BEFORE INSERT ON reading_items " +
                    "BEGIN SELECT RAISE(ABORT, 'test write failure'); END",
            )
            try {
                h.repository.refresh()
                error("Cache write must fail")
            } catch (_: ReadingRefreshException) { }
            h.executeSql("DROP TRIGGER fail_reading_save")
            val cover = h.createCover()
            h.load = { config ->
                val value = snapshot(config, "A.epub")
                value.copy(
                    history = listOf(checkNotNull(value.current)),
                    metadata = mapOf(
                        "A.epub" to BookMetadata("known", "A", null, cover.toURI().toString()),
                    ),
                )
            }
            h.repository.refresh()
            h.repository.refresh()
            assertEquals(2, h.reads)
            check(cover.delete())
            h.repository.refresh()
            assertEquals(3, h.reads)
        }
    }

    @Test fun refreshCapturesAttemptTimeBeforeReadAndCandidateUsesCurrentClock() = runBlocking {
        withHarness { h ->
            h.time = 100L
            h.load = {
                h.time = 200L
                snapshot(it, "A.epub")
            }
            h.repository.refresh()
            assertEquals(100L, h.repository.sync.first { it.lastAttemptAt == 100L }.lastAttemptAt)
            h.time = 300L
            h.load = { snapshot(it, "B.epub") }
            val before = h.config.config.first()
            val candidate = h.config.treeCandidate(
                before,
                SourceFolder.Books,
                "content://test/other",
            )
            h.repository.replaceSource(before, candidate)
            assertCurrent(h.repository, "B.epub")
            val saved = h.repository.sync.first { it.lastAttemptAt == 300L }
            h.time = 400L
            h.load = { throw ReadingRefreshException(ReadingSyncIssue.PermissionLost) }
            try {
                h.repository.refresh()
            } catch (_: ReadingRefreshException) { }
            val failed = h.repository.sync.first {
                it.error == ReadingSyncIssue.PermissionLost && it.lastAttemptAt == 400L
            }
            assertEquals(400L, failed.lastAttemptAt)
            assertEquals(saved.lastSuccessAt, failed.lastSuccessAt)
            assertCurrent(h.repository, "B.epub")
        }
    }

    @Test fun currentChangesAndDiskCacheSurviveRepositoryRestart() = runBlocking {
        withHarness { h ->
            for (path in listOf("A.epub", "B.epub", "A.epub")) {
                h.load = { snapshot(it, path) }
                h.repository.refresh()
                assertCurrent(h.repository, path)
            }
            val success = h.repository.sync.first { it.lastSuccessAt != null }
            assertNotNull(success.lastAttemptAt)
            val reads = h.reads
            h.reopen()
            assertCurrent(h.repository, "A.epub")
            assertEquals(reads, h.reads)
        }
    }

    @Test fun sharedRefreshSurvivesOneCancelledCallerAndRecoversFromFailure() = runBlocking {
        withHarness { h ->
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            h.load =
                { config ->
                    assertEquals(
                        "reading-permissions-1",
                        currentCoroutineContext()[CoroutineName]?.name,
                    )
                    started.complete(Unit)
                    release.await()
                    snapshot(config, "A.epub")
                }
            val first = async(start = CoroutineStart.UNDISPATCHED) { h.repository.refresh() }
            val second = async(start = CoroutineStart.UNDISPATCHED) { h.repository.refresh() }
            started.await()
            first.cancelAndJoin()
            release.complete(Unit)
            second.await()
            assertEquals(1, h.reads)
            assertEquals(1, h.permissionSnapshots)
            assertCurrent(h.repository, "A.epub")
            h.load = { throw ReadingRefreshException(ReadingSyncIssue.PermissionLost) }
            try {
                h.repository.refresh()
            } catch (_: ReadingRefreshException) { }
            val failed = h.repository.sync.first { it.error == ReadingSyncIssue.PermissionLost }
            assertEquals(2, h.permissionSnapshots)
            assertTrue(failed.stale)
            assertCurrent(h.repository, "A.epub")
            h.load = { snapshot(it, "B.epub") }
            h.repository.refresh()
            assertCurrent(h.repository, "B.epub")
            h.repository.sync.first { !it.running && it.error == null }
            assertEquals(3, h.permissionSnapshots)
        }
    }

    @Test fun failedCandidateAndCancelledOldRefreshCannotReplaceActiveSource() = runBlocking {
        withHarness { h ->
            h.repository.refresh()
            val before = h.config.config.first()
            val bad = h.config.treeCandidate(before, SourceFolder.KOReader, "content://test/bad")
            h.load = { throw ReadingRefreshException(ReadingSyncIssue.InvalidData) }
            try {
                h.repository.replaceSource(before, bad)
            } catch (_: ReadingRefreshException) { }
            assertEquals(before, h.config.config.first())
            assertCurrent(h.repository, "A.epub")
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            h.load = { config ->
                if (config.booksTreeUris == before.booksTreeUris) {
                    withContext(NonCancellable) {
                        started.complete(Unit)
                        withTimeout(5_000) { release.await() }
                    }
                    snapshot(config, "OLD.epub")
                } else {
                    snapshot(config, "B.epub")
                }
            }
            val old = async { h.repository.refresh() }
            started.await()
            val candidate = h.config.treeCandidate(before, SourceFolder.Books, "content://test/new")
            h.repository.replaceSource(before, candidate)
            assertEquals(before.sourceId, candidate.sourceId)
            assertCurrent(h.repository, "B.epub")
            release.complete(Unit)
            old.join()
            assertCurrent(h.repository, "B.epub")
            assertEquals(
                "A.epub",
                h.database.readingDao().get(
                    checkNotNull(before.sourceId),
                    checkNotNull(before.readingCacheKey()).snapshotKey,
                )?.book?.path,
            )
        }
    }

    @Test fun onlySuccessfulReadCanPublishEmptyAndDisconnectClearsCacheOnly() = runBlocking {
        withHarness { h ->
            h.load = { throw ReadingRefreshException(ReadingSyncIssue.InvalidData) }
            try {
                h.repository.refresh()
            } catch (_: ReadingRefreshException) { }
            h.repository.reading.first { it is ReadingState.Error }
            h.load = { CurrentReadingSnapshot(null) }
            h.repository.refresh()
            h.repository.reading.first { it == ReadingState.Empty }
            val user = DataStoreUserPreferencesRepository(h.store)
            user.pin("reader/Activity")
            val before = h.config.config.first()
            h.repository.disconnect()
            h.repository.reading.first { it == ReadingState.NotConfigured }
            assertEquals(listOf("reader/Activity"), user.pinnedComponentIds.first())
            assertEquals(
                null,
                h.database.readingDao().get(
                    checkNotNull(before.sourceId),
                    checkNotNull(before.readingCacheKey()).snapshotKey,
                ),
            )
        }
    }

    private suspend fun assertCurrent(repository: RoomReadingRepository, path: String) {
        repository.reading.first { it is ReadingState.Content && it.current.id.path == path }
    }

    private fun snapshot(config: ReadingSourceConfig, path: String) = CurrentReadingSnapshot(
        ReadingItem(ReadingItemId(checkNotNull(config.sourceId), path), path, null, null),
    )

    private suspend fun withHarness(test: suspend (Harness) -> Unit) = withTimeout(20_000) {
        val h = Harness()
        try {
            for ((folder, uri) in listOf(
                SourceFolder.KOReader to "content://test/reader",
                SourceFolder.Books to "content://test/books",
            )) {
                val before = h.config.config.first()
                h.config.activateTree(before, h.config.treeCandidate(before, folder, uri))
            }
            test(h)
        } finally {
            h.close()
        }
    }

    private inner class Harness {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val directory = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "reading-cache-test-",
        ).toFile()
        private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = storeScope, produceFile = {
            File(directory, "settings.preferences_pb")
        })
        val config = ReadingSourceConfigRepository(store)
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var database = openDatabase()
        var reads = 0
        var permissionSnapshots = 0
        var fingerprint: SourceMetadataFingerprint? = null
        var permitted = true
        var checkFingerprint: suspend (SourceMetadataFingerprint) -> Boolean = {
            permitted && fingerprint == it
        }
        var time = 1L
        var load: suspend (
            ReadingSourceConfig,
        ) -> CurrentReadingSnapshot = { snapshot(it, "A.epub") }
        private val covers = FileBookCoverStore(File(directory, "covers"))
        var repository = createRepository()

        fun enableSkip() {
            time = 100_000
            fingerprint = fingerprint(1)
            load = { config ->
                val value = snapshot(config, "A.epub")
                value.copy(history = listOf(checkNotNull(value.current)))
            }
        }

        fun createCover(): File = File(directory, "covers/v1-${"a".repeat(64)}.webp").apply {
            checkNotNull(parentFile).apply { check(mkdirs() || isDirectory) }
            writeText("fixture")
        }

        suspend fun executeSql(sql: String) = withContext(Dispatchers.IO) {
            database.readingDao().get("fixture", "fixture")
            AndroidSQLiteDriver().open(File(directory, "reading.db").path).use { connection ->
                connection.prepare(sql).use { it.step() }
            }
        }
        private fun openDatabase() =
            Room.databaseBuilder<ReadingDatabase>(context, File(directory, "reading.db").path)
                .setDriver(AndroidSQLiteDriver()).build()
        private fun createRepository() = RoomReadingRepository(
            config,
            ReadingCache(database.readingDao()),
            object : CurrentReadingDataSource {
                override suspend fun read(config: ReadingSourceConfig): CurrentReadingSnapshot {
                    reads++
                    if (!permitted) throw ReadingRefreshException(ReadingSyncIssue.PermissionLost)
                    return load(config).copy(fingerprint = fingerprint)
                }

                override suspend fun unchanged(
                    config: ReadingSourceConfig,
                    fingerprint: SourceMetadataFingerprint,
                ): Boolean {
                    assertEquals(
                        "reading-permissions-$permissionSnapshots",
                        currentCoroutineContext()[CoroutineName]?.name,
                    )
                    return checkFingerprint(fingerprint)
                }
            },
            scope,
            covers = covers,
            now = { time },
            permissionSnapshot = { CoroutineName("reading-permissions-${++permissionSnapshots}") },
        )
        suspend fun reopen() {
            scope.coroutineContext.job.cancelAndJoin()
            database.close()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            database = openDatabase()
            repository = createRepository()
        }
        suspend fun close() {
            withContext(NonCancellable) {
                scope.coroutineContext.job.cancelAndJoin()
                storeScope.coroutineContext.job.cancelAndJoin()
                database.close()
                check(directory.canonicalFile.parentFile == context.cacheDir.canonicalFile)
                directory.deleteRecursively()
            }
        }
    }

    private fun fingerprint(modified: Long) = SourceMetadataFingerprint(
        mapOf(
            SourceMetadataFingerprint.Document("content://test/reader", listOf("history.lua")) to
                SourceDocument("content://test/history", null, 1, modified),
        ),
    )
}
