package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.local.reading.LibraryBookEntity
import dev.twotechy.inknest.data.local.reading.LibraryFolderEntity
import dev.twotechy.inknest.data.local.reading.LibraryFolderKey
import dev.twotechy.inknest.data.local.reading.LibraryInventoryStore
import dev.twotechy.inknest.data.source.BookCatalogSource
import dev.twotechy.inknest.data.source.CatalogBook
import dev.twotechy.inknest.data.source.EnrichedReadingItem
import dev.twotechy.inknest.data.source.SourceDocument
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FolderLibraryRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun addingRootDoesNotRescanExistingRootAndFailedRootKeepsCache() = runBlocking {
        withTimeout(10000) {
            withHarness { h ->
                h.add("A")
                h.loaded(1)
                val reads = h.reads["A"]
                h.add("B")
                val merged = h.loaded(2)
                assertEquals(reads, h.reads["A"])
                assertEquals(2, merged.books.map { it.id }.distinct().size)
                assertEquals(1, merged.books.map { it.title }.distinct().size)
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

    private suspend fun withHarness(block: suspend (Harness) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            block(Harness(folder.newFolder(), scope))
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    private class Harness(directory: File, scope: CoroutineScope) {
        val config =
            ReadingSourceConfigRepository(
                testPreferencesDataStore(File(directory, "source.preferences_pb"), scope),
            )
        val inventory = MemoryLibraryInventoryStore()
        val reads = ConcurrentHashMap<String, Int>()
        val responses = ConcurrentHashMap<String, suspend () -> List<CatalogBook>>()
        private val reading = object : ReadingRepository {
            override val reading = flowOf(ReadingState.Empty)
            override suspend fun refresh() = Unit
        }
        val repository = FolderLibraryRepository(
            config,
            inventory,
            BookCatalogSource { tree ->
                reads.merge(tree, 1, Int::plus)
                responses[tree]?.invoke() ?: listOf(catalogBook(tree))
            },
            scope,
            reading,
        ) { _, _, item -> EnrichedReadingItem(item, null) }

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
