package dev.twotechy.inknest.data

import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.InkNestApplication
import dev.twotechy.inknest.data.local.reading.ReadingDatabase
import dev.twotechy.inknest.data.repository.FolderLibraryRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.ReadingState
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.source.BookCatalogSource
import dev.twotechy.inknest.data.source.BookMetadata
import dev.twotechy.inknest.data.source.CatalogBook
import dev.twotechy.inknest.data.source.EnrichedReadingItem
import dev.twotechy.inknest.data.source.SafTreeReader
import dev.twotechy.inknest.data.source.SourceDocument
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingStatus
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

class FolderLibraryIntegrationTest {
    @Test fun inventoryIncludesUnreadBooksRetainsCacheOnFailureAndSurvivesRestart() = runBlocking {
        withTimeout(20_000) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val directory = Files.createTempDirectory(
                context.cacheDir.toPath(),
                "folder-test-",
            ).toFile()
            val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            fun openDatabase() = Room.databaseBuilder<ReadingDatabase>(
                context,
                File(directory, "library.db").path,
            )
                .setDriver(AndroidSQLiteDriver()).build()
            var database = openDatabase()
            try {
                val store = PreferenceDataStoreFactory.create(scope = storeScope) {
                    File(directory, "source.preferences_pb")
                }
                val config = ReadingSourceConfigRepository(store)
                val before = config.config.first()
                val candidate = config.treeCandidate(
                    before,
                    SourceFolder.Books,
                    "content://test/books",
                )
                config.activateTree(before, candidate)
                val source = FilesSource()
                val history = History()
                fun repository() = FolderLibraryRepository(
                    config,
                    database.libraryDao(),
                    source,
                    scope,
                    history,
                    enrich = { _, _, item ->
                        EnrichedReadingItem(
                            item.copy(title = "Same title", author = "File author"),
                            BookMetadata("fingerprint", "Same title", "File author", null),
                        )
                    },
                )
                val repository = repository()
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
                source.fail = true
                repository.refresh()
                val failed = repository.catalog.first {
                    it.sync.error ==
                        ReadingSyncIssue.PermissionLost
                }
                assertEquals(merged.books, failed.books)
                assertTrue(failed.sync.stale)
                source.fail = false
                source.entries = source.entries.take(1)
                repository.refresh()
                repository.catalog.first {
                    it.books.size == 1 && !it.sync.running &&
                        it.sync.error == null
                }
                val reads = source.reads
                scope.coroutineContext.job.cancel()
                scope.coroutineContext.job.join()
                database.close()
                database = openDatabase()
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                val reopened = repository()
                val cached = reopened.catalog.first { it.books.size == 1 && !it.sync.running }
                assertEquals("Same title", cached.books.single().title)
                assertEquals(reads, source.reads)
                val readingCache = dev.twotechy.inknest.data.repository.CachedReadingRepository(
                    config,
                    dev.twotechy.inknest.data.local.reading.ReadingCache(database.readingDao()),
                    dev.twotechy.inknest.data.source.CurrentReadingSource {
                        dev.twotechy.inknest.data.source.CurrentReadingSnapshot(null)
                    },
                    scope,
                )
                readingCache.clearCache()
                database.libraryDao().observeFolder(
                    checkNotNull(config.config.first().sourceId),
                    "content://test/books",
                ).first { it?.complete == true }
                assertTrue(source.reads > reads)
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
                    config.treeCandidate(
                        previous,
                        SourceFolder.Books,
                        "content://test/other",
                    ),
                )
                val changed = reopened.catalog.first {
                    it.books.singleOrNull()?.id?.path == "primary:Other/C.epub" && !it.sync.running
                }
                assertNull(changed.books.single().progress)
            } finally {
                scope.coroutineContext.job.cancel()
                scope.coroutineContext.job.join()
                storeScope.coroutineContext.job.cancel()
                storeScope.coroutineContext.job.join()
                database.close()
                directory.deleteRecursively()
            }
        }
    }

    @Test fun directDocumentReadRejectsEscapingTheGrantedBooksTree() = runBlocking {
        assumeTrue(
            InstrumentationRegistry.getArguments().getString("library_real_source") == "true",
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = (context.applicationContext as InkNestApplication).container
        val config = checkNotNull(container.readingSourceConfigRepository).config.first()
        val tree = Uri.parse(checkNotNull(config.booksTreeUri))
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
    private class FilesSource : BookCatalogSource {
        var fail = false
        var reads = 0
        var entries = listOf("A.epub", "nested/B.epub").map {
            CatalogBook(
                "primary:Books/$it",
                it.split('/'),
                SourceDocument("content://test/$it", null, 1, 1),
            )
        }
        override suspend fun scan(tree: String): List<CatalogBook> {
            reads++
            if (fail) throw SecurityException("Permission lost")
            return entries
        }
    }
}
