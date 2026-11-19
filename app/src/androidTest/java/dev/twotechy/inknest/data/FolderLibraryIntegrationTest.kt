package dev.twotechy.inknest.data

import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.InkNestApplication
import dev.twotechy.inknest.data.local.database.ReadingDatabase
import dev.twotechy.inknest.data.local.database.library.RoomLibraryInventoryStore
import dev.twotechy.inknest.data.local.database.reading.ReadingCache
import dev.twotechy.inknest.data.repository.library.impl.SafLibraryRepository
import dev.twotechy.inknest.data.repository.reading.ReadingRepository
import dev.twotechy.inknest.data.repository.reading.ReadingState
import dev.twotechy.inknest.data.repository.reading.impl.RoomReadingRepository
import dev.twotechy.inknest.data.repository.sources.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.source.books.BookCatalogDataSource
import dev.twotechy.inknest.data.source.books.BookMetadata
import dev.twotechy.inknest.data.source.books.CatalogBook
import dev.twotechy.inknest.data.source.books.ReadingMetadataResult
import dev.twotechy.inknest.data.source.koreader.CurrentReadingDataSource
import dev.twotechy.inknest.data.source.koreader.CurrentReadingSnapshot
import dev.twotechy.inknest.data.source.saf.SourceDocument
import dev.twotechy.inknest.data.source.saf.impl.SafTreeReader
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingStatus
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

class FolderLibraryIntegrationTest {
    @Test fun inventoryIncludesUnreadBooksRetainsCacheOnFailureAndSurvivesRestart() = runBlocking {
        withFixture { fixture ->
            fixture.verifyInventoryAndHistory()
            fixture.verifyFailureAndDeletion()
            fixture.verifyRestartAndCacheClear()
            fixture.verifySourceIsolation()
        }
    }

    private suspend fun withFixture(test: suspend (Fixture) -> Unit) =
        withTimeout(20_000.milliseconds) {
            val fixture = Fixture()
            try {
                fixture.prepare()
                test(fixture)
            } finally {
                fixture.close()
            }
        }

    private class Fixture {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val directory = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "folder-test-",
        ).toFile()
        private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var database = openDatabase()
        private val store = PreferenceDataStoreFactory.create(scope = storeScope) {
            File(directory, "source.preferences_pb")
        }
        private val config = ReadingSourceConfigRepository(store)
        private val source = FilesSource()
        private val history = History()
        private lateinit var repository: SafLibraryRepository

        suspend fun prepare() {
            val before = config.config.first()
            val candidate = config.treeCandidate(before, SourceFolder.Books, "content://test/books")
            config.activateTree(before, candidate)
            repository = createRepository()
        }

        suspend fun verifyInventoryAndHistory() {
            repository.refresh()
            val all = repository.catalog.first { it.books.size == 2 && !it.sync.running }
            assertTrue(all.configured)
            assertTrue(all.books.all { it.progress == null })
            assertEquals(2, all.books.map { it.id }.distinct().size)
            val id = all.books.first().id
            history.trackedItems.value = listOf(
                ReadingItem(id, "Reader title", null, 70, status = ReadingStatus.Reading),
                ReadingItem(
                    ReadingItemId(id.sourceId, "primary:outside.epub"),
                    "Outside",
                    null,
                    10,
                ),
            )
            val merged = repository.catalog.first { it.books.any { it.progress == 70 } }
            assertEquals(2, merged.books.size)
            assertTrue(merged.books.all { it.title == "Same title" })
            assertNull(merged.books.last().progress)
        }

        suspend fun verifyFailureAndDeletion() {
            val previous = repository.catalog.first { it.books.any { it.progress == 70 } }.books
            source.fail = true
            repository.refresh()
            val failed = repository.catalog.first {
                it.sync.error == ReadingSyncIssue.PermissionLost
            }
            assertEquals(previous, failed.books)
            assertTrue(failed.sync.stale)
            source.fail = false
            source.entries = source.entries.take(1)
            repository.refresh()
            repository.catalog.first {
                it.books.size == 1 && !it.sync.running && it.sync.error == null
            }
        }

        suspend fun verifyRestartAndCacheClear() {
            val reads = source.reads
            scope.coroutineContext.job.cancelAndJoin()
            database.close()
            database = openDatabase()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            repository = createRepository()
            val cached = repository.catalog.first { it.books.size == 1 && !it.sync.running }
            assertEquals("Same title", cached.books.single().title)
            assertEquals(reads, source.reads)
            val readingCache = RoomReadingRepository(
                config,
                ReadingCache(database.readingDao()),
                CurrentReadingDataSource { CurrentReadingSnapshot(null) },
                scope,
            )
            readingCache.clearCache()
            database.libraryDao().observeFolder(
                checkNotNull(config.config.first().sourceId),
                "content://test/books",
            ).first { it?.complete == true }
            assertTrue(source.reads > reads)
        }

        suspend fun verifySourceIsolation() {
            val previous = config.config.first()
            source.entries = listOf(
                CatalogBook(
                    "primary:Other/C.epub",
                    listOf("C.epub"),
                    SourceDocument("content://test/C.epub", null, 1, 1),
                ),
            )
            config.activateTree(
                previous,
                previous.copy(booksTreeUris = setOf("content://test/other")),
            )
            val changed = repository.catalog.first {
                it.books.singleOrNull()?.id?.path == "primary:Other/C.epub" && !it.sync.running
            }
            assertNull(changed.books.single().progress)
        }

        suspend fun close() = withContext(NonCancellable) {
            scope.coroutineContext.job.cancelAndJoin()
            storeScope.coroutineContext.job.cancelAndJoin()
            database.close()
            directory.deleteRecursively()
        }

        private fun openDatabase() = Room.databaseBuilder<ReadingDatabase>(
            context,
            File(directory, "library.db").path,
        ).setDriver(AndroidSQLiteDriver()).build()

        private fun createRepository() = SafLibraryRepository(
            config,
            RoomLibraryInventoryStore(database.libraryDao()),
            source,
            scope,
            history,
            readMetadata = { _, _, item ->
                ReadingMetadataResult(
                    item.copy(title = "Same title", author = "File author"),
                    BookMetadata("fingerprint", "Same title", "File author", null),
                )
            },
        )
    }

    @Test fun directDocumentReadRejectsEscapingTheGrantedBooksTree() = runBlocking {
        assumeTrue(
            InstrumentationRegistry.getArguments().getString("library_real_source") == "true",
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = (context.applicationContext as InkNestApplication).container
        val config = checkNotNull(container.readingSourceConfigRepository).config.first()
        val tree = config.booksTreeUris.first().toUri()
        assumeTrue(DocumentsContract.getTreeDocumentId(tree) != "primary:")
        val outside = DocumentsContract.buildDocumentUriUsingTree(
            tree,
            "primary:__outside__/book.epub",
        )
        try {
            val document = SourceDocument(outside.toString(), null, null, null)
            SafTreeReader(context.contentResolver).readDocument(document) {
                fail("Outside documents must not be opened")
            }
            fail("Expected containment failure")
        } catch (error: IOException) {
            assertEquals("Outside tree", error.message)
        }
    }

    private class History : ReadingRepository {
        override val reading = MutableStateFlow<ReadingState>(ReadingState.NotConfigured)
        override val trackedItems = MutableStateFlow<List<ReadingItem>>(emptyList())
        override suspend fun refresh() = Unit
    }
    private class FilesSource : BookCatalogDataSource {
        var fail = false
        var reads = 0
        var entries = listOf("A.epub", "nested/B.epub").map {
            CatalogBook(
                "primary:Books/$it",
                it.split('/'),
                SourceDocument("content://test/$it", null, 1, 1),
            )
        }
        override suspend fun scanBooks(tree: String): List<CatalogBook> {
            reads++
            if (fail) throw SecurityException("Permission lost")
            return entries
        }
    }
}
