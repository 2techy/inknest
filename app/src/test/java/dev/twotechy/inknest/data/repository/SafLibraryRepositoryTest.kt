package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.local.reading.LibraryBookEntity
import dev.twotechy.inknest.data.local.reading.LibraryFolderEntity
import dev.twotechy.inknest.data.local.reading.LibraryFolderKey
import dev.twotechy.inknest.data.local.reading.LibraryInventoryStore
import dev.twotechy.inknest.data.source.books.BookCatalogDataSource
import dev.twotechy.inknest.data.source.books.CatalogBook
import dev.twotechy.inknest.data.source.books.ReadingMetadataResult
import dev.twotechy.inknest.data.source.saf.SourceDocument
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SafLibraryRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun scanTimestampUsesSharedClockAndFailedScanRetainsPreviousTime() =
        runTest(timeout = 10.seconds) {
            withHarness(StandardTestDispatcher(testScheduler)) { h ->
                h.time = 100L
                h.add("A")
                h.loaded(1)
                // A published idle state can precede completion of the shared scan job.
                runCurrent()
                val key = LibraryFolderKey(checkNotNull(h.config.config.first().sourceId), "A")
                assertEquals(100L, h.inventory.observeFolder(key).first()?.updatedAt)
                h.time = 200L
                h.repository.refresh()
                assertEquals(200L, h.inventory.observeFolder(key).first()?.updatedAt)
                h.time = 300L
                h.responses["A"] = { throw SecurityException("grant lost") }
                h.repository.refresh()
                val failed = h.repository.catalog.first {
                    it.sync.error ==
                        ReadingSyncIssue.PermissionLost
                }
                assertTrue(failed.sync.stale)
                assertEquals(200L, h.inventory.observeFolder(key).first()?.updatedAt)
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun queuedResumeSkipsFolderCompletedWhileWaitingForScanGate() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val config = ReadingSourceConfigRepository(
                testPreferencesDataStore(File(folder.newFolder(), "source.preferences_pb"), scope),
            )
            val before = config.config.first()
            config.activateTree(before, config.treeCandidate(before, SourceFolder.Books, "A"))
            val key = LibraryFolderKey(checkNotNull(config.config.first().sourceId), "A")
            val inventory = MemoryLibraryInventoryStore()
            val lockedGate = Mutex(locked = true)
            val resumeQueued = CompletableDeferred<Unit>()
            val gate = object : Mutex by lockedGate {
                override suspend fun lock(owner: Any?) {
                    resumeQueued.complete(Unit)
                    lockedGate.lock(owner)
                }
            }
            var scans = 0
            val reading = object : ReadingRepository {
                override val reading = flowOf(ReadingState.Empty)
                override suspend fun refresh() = Unit
            }
            val repository = SafLibraryFolderRepository(
                config,
                "A",
                gate,
                inventory,
                BookCatalogDataSource {
                    scans++
                    listOf(catalogBook("A"))
                },
                scope,
                reading,
            ) { _, _, item -> ReadingMetadataResult(item, null) }
            resumeQueued.await()
            inventory.replace(
                LibraryFolderEntity(key.source, key.tree, 1, true),
                listOf(LibraryBookEntity(key.source, key.tree, "primary:A/Book.epub", "Book")),
            )
            runCurrent()
            gate.unlock()
            runCurrent()
            repository.catalog.first { !it.loading && !it.sync.running }
            assertEquals(0, scans)
            repository.refresh()
            assertEquals("Explicit refresh must still scan a complete folder", 1, scans)
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun addingRootDoesNotRescanExistingRootAndFailedRootKeepsCache() =
        runTest(timeout = 10.seconds) {
            withHarness(StandardTestDispatcher(testScheduler)) { h ->
                h.add("A")
                h.loaded(1)
                val reads = h.reads["A"]
                h.add("B")
                val merged = h.loaded(2)
                assertEquals(reads, h.reads["A"])
                assertEquals(2, merged.books.map { it.id }.distinct().size)
                assertEquals(1, merged.books.map { it.title }.distinct().size)
                // A published idle state can precede completion of the shared scan job.
                runCurrent()
                h.responses["B"] = { throw SecurityException("grant lost") }
                h.repository.refresh()
                val stale = h.repository.catalog.first {
                    it.folders.any { root -> root.sync.error == ReadingSyncIssue.PermissionLost } &&
                        !it.sync.running
                }
                assertEquals(2, stale.books.size)
                assertTrue(stale.folders.first { it.uri == "B" }.sync.stale)
                assertEquals(null, stale.folders.first { it.uri == "A" }.sync.error)
            }
        }

    @Test fun removingRootWhileProviderFinishesCannotRestoreRemovedBooks() = runBlocking {
        withTimeout(10000) {
            withHarness { h ->
                h.add("A")
                h.loaded(1)
                val started = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                h.responses["B"] = {
                    withContext(NonCancellable) {
                        started.complete(Unit)
                        release.await()
                    }
                    listOf(catalogBook("B"))
                }
                h.add("B")
                started.await()
                val before = h.config.config.first()
                h.config.activateTree(before, before.copy(booksTreeUris = setOf("A")))
                release.complete(Unit)
                val remaining = h.repository.catalog.first {
                    it.folders.map { root -> root.uri } == listOf("A") && !it.loading
                }
                assertEquals(listOf("primary:A/Book.epub"), remaining.books.map { it.id.path })
                assertTrue(
                    h.inventory.books(
                        LibraryFolderKey(checkNotNull(before.sourceId), "B"),
                    ).isEmpty(),
                )
            }
        }
    }

    @Test fun observationFailureRetainsBooksAndRecoveryDoesNotScanSaf() = runBlocking {
        withTimeout(10_000) {
            withHarness { h ->
                h.add("A")
                val loaded = h.loaded(1)
                val reads = h.reads["A"]
                h.inventory.unreadable.value = true
                val failed = h.repository.catalog.first { it.sync.error == ReadingSyncIssue.Cache }
                assertEquals(loaded.books, failed.books)
                assertTrue(failed.sync.stale)
                h.inventory.unreadable.value = false
                val recovered = h.repository.catalog.first { !it.loading && it.sync.error == null }
                assertEquals(loaded.books, recovered.books)
                assertEquals(reads, h.reads["A"])
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun cancellationStopsBeforeTheNextBookInTheBatch() = runTest(timeout = 10.seconds) {
        withHarness(StandardTestDispatcher(testScheduler)) { h ->
            h.responses["A"] = {
                List(2) { index ->
                    catalogBook("A").copy(
                        path = "primary:A/Book$index.epub",
                        relative = listOf("Book$index.epub"),
                    )
                }
            }
            val metadataReads = mutableListOf<String>()
            h.readMetadata = { item ->
                metadataReads += item.id.path
                currentCoroutineContext().cancel()
                ReadingMetadataResult(item, null)
            }
            h.add("A")
            runCurrent()
            assertTrue(metadataReads.isNotEmpty())
            assertTrue(metadataReads.all { it == "primary:A/Book0.epub" })
            val key = LibraryFolderKey(checkNotNull(h.config.config.first().sourceId), "A")
            assertEquals(2, h.inventory.books(key).size)
            assertEquals(false, h.inventory.observeFolder(key).first()?.complete)
        }
    }

    private suspend fun withHarness(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: suspend (Harness) -> Unit,
    ) {
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            block(Harness(folder.newFolder(), scope))
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    private class Harness(directory: File, scope: CoroutineScope) {
        var time = 1L
        val config =
            ReadingSourceConfigRepository(
                testPreferencesDataStore(File(directory, "source.preferences_pb"), scope),
            )
        val inventory = MemoryLibraryInventoryStore()
        val reads = ConcurrentHashMap<String, Int>()
        val responses = ConcurrentHashMap<String, suspend () -> List<CatalogBook>>()
        var readMetadata: suspend (ReadingItem) -> ReadingMetadataResult = {
            ReadingMetadataResult(it, null)
        }
        private val reading = object : ReadingRepository {
            override val reading = flowOf(ReadingState.Empty)
            override suspend fun refresh() = Unit
        }
        val repository = SafLibraryRepository(
            config,
            inventory,
            BookCatalogDataSource { tree ->
                reads.merge(tree, 1, Int::plus)
                responses[tree]?.invoke() ?: listOf(catalogBook(tree))
            },
            scope,
            reading,
            now = { time },
        ) { _, _, item -> readMetadata(item) }

        suspend fun add(tree: String) {
            val before = config.config.first()
            config.activateTree(before, config.treeCandidate(before, SourceFolder.Books, tree))
        }
        suspend fun loaded(count: Int): LibraryCatalog = repository.catalog.first {
            it.books.size == count && it.folders.size == count && !it.loading && !it.sync.running
        }
    }

    companion object {
        private fun catalogBook(tree: String) = CatalogBook(
            "primary:$tree/Book.epub",
            listOf("Book.epub"),
            SourceDocument("content://test/$tree/book", null, 1, 1),
        )
    }
}

private class MemoryLibraryInventoryStore : LibraryInventoryStore {
    private data class Rows(
        val books: List<LibraryBookEntity> = emptyList(),
        val folder: LibraryFolderEntity? = null,
    )
    private val rows = MutableStateFlow<Map<LibraryFolderKey, Rows>>(emptyMap())
    val unreadable = MutableStateFlow(false)
    override fun observe(key: LibraryFolderKey) = combine(rows, unreadable) { rows, failed ->
        if (failed) throw IOException("cache unavailable")
        rows[key]?.books.orEmpty()
    }
    override fun observeFolder(key: LibraryFolderKey) = rows.map { it[key]?.folder }
    override suspend fun books(key: LibraryFolderKey) = rows.value[key]?.books.orEmpty()
    override suspend fun putBooks(books: List<LibraryBookEntity>) {
        books.groupBy { LibraryFolderKey(it.sourceId, it.folderKey) }.forEach { (key, batch) ->
            rows.update { current ->
                val previous = current[key] ?: Rows()
                val paths = batch.map { it.path }.toSet()
                val retained = previous.books.filter { it.path !in paths }
                current + (key to previous.copy(books = retained + batch))
            }
        }
    }
    override suspend fun putFolder(folder: LibraryFolderEntity) {
        val key = LibraryFolderKey(folder.sourceId, folder.folderKey)
        rows.update { it + (key to (it[key] ?: Rows()).copy(folder = folder)) }
    }
    override suspend fun remove(key: LibraryFolderKey) {
        rows.update { it - key }
    }
    override suspend fun replace(folder: LibraryFolderEntity, books: List<LibraryBookEntity>) {
        val key = LibraryFolderKey(folder.sourceId, folder.folderKey)
        rows.update { it + (key to Rows(books, folder)) }
    }
}
