package dev.twotechy.inknest.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.reading.ReadingCache
import dev.twotechy.inknest.data.local.reading.ReadingDatabase
import dev.twotechy.inknest.data.local.reading.readingCacheKey
import dev.twotechy.inknest.data.repository.CachedReadingRepository
import dev.twotechy.inknest.data.repository.DataStoreUserPreferencesRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.source.CurrentReadingSnapshot
import dev.twotechy.inknest.data.source.CurrentReadingSource
import dev.twotechy.inknest.data.source.ReadingRefreshException
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
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
            assertCurrent(h.repository, "A.epub")
            h.load = { throw ReadingRefreshException(ReadingSyncIssue.PermissionLost) }
            try {
                h.repository.refresh()
            } catch (_: ReadingRefreshException) { }
            val failed = h.repository.sync.first { it.error == ReadingSyncIssue.PermissionLost }
            assertTrue(failed.stale)
            assertCurrent(h.repository, "A.epub")
            h.load = { snapshot(it, "B.epub") }
            h.repository.refresh()
            assertCurrent(h.repository, "B.epub")
            h.repository.sync.first { !it.running && it.error == null }
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

    private suspend fun assertCurrent(repository: CachedReadingRepository, path: String) {
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
        var load: suspend (
            ReadingSourceConfig,
        ) -> CurrentReadingSnapshot = { snapshot(it, "A.epub") }
        var repository = createRepository()
        private fun openDatabase() =
            Room.databaseBuilder<ReadingDatabase>(context, File(directory, "reading.db").path)
                .setDriver(AndroidSQLiteDriver()).build()
        private fun createRepository() = CachedReadingRepository(
            config,
            ReadingCache(database.readingDao()),
            CurrentReadingSource {
                reads++
                load(it)
            },
            scope,
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
}
