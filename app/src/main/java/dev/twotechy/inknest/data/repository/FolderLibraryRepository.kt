package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.local.reading.LibraryFolderKey
import dev.twotechy.inknest.data.local.reading.LibraryInventoryStore
import dev.twotechy.inknest.data.source.BookCatalogSource
import dev.twotechy.inknest.data.source.EnrichedReadingItem
import dev.twotechy.inknest.data.source.ResolvedBook
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncStatus
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
internal class FolderLibraryRepository(
    private val config: ReadingSourceConfigRepository,
    private val inventoryStore: LibraryInventoryStore,
    private val source: BookCatalogSource,
    private val scope: CoroutineScope,
    private val reading: ReadingRepository,
    private val enrich: suspend (
        ReadingSourceConfig,
        ResolvedBook,
        ReadingItem,
    ) -> EnrichedReadingItem,
) : LibraryRepository {
    private data class Worker(val lifetime: Job, val repository: LibraryFolderRepository)
    private val workers = MutableStateFlow<Map<LibraryFolderKey, Worker>>(emptyMap())
    private val scanGate = Mutex()
    private val requests = Mutex()
    private var refreshJob: Deferred<Unit>? = null
    private val selection =
        combine(config.config, workers) { selected, active -> selected to active }
    private val refreshing = MutableStateFlow(false)
    private val inventory = selection.flatMapLatest { (selected, active) ->
        val trees = selected.booksTreeUris.sorted()
        if (trees.isEmpty()) {
            flowOf(LibraryCatalog())
        } else {
            val flows = trees.map { tree ->
                active[LibraryFolderKey(selected.sourceId.orEmpty(), tree)]?.repository?.catalog
                    ?: flowOf(LibraryCatalog(configured = true, loading = true))
            }
            combine(flows) { catalogs -> mergeCatalogs(trees, catalogs.toList()) }
        }
    }
    override val catalog = combine(inventory, refreshing) { catalog, refreshing ->
        catalog.copy(sync = catalog.sync.copy(running = catalog.sync.running || refreshing))
    }.stateIn(scope, SharingStarted.Eagerly, LibraryCatalog(loading = true))

    private fun matches(selected: ReadingSourceConfig, roots: Set<LibraryFolderKey>): Boolean =
        roots.map { it.tree }.toSet() == selected.booksTreeUris &&
            roots.all { it.source == selected.sourceId }

    init {
        scope.launch { config.config.collect { reconcile(it) } }
    }

    private suspend fun reconcile(selected: ReadingSourceConfig) {
        val keys = selected.sourceId?.let { id ->
            selected.booksTreeUris.sorted().map { LibraryFolderKey(id, it) }.toSet()
        }.orEmpty()
        val current = workers.value
        val retained = current.filterKeys { it in keys }.toMutableMap()
        workers.value = retained.toMap()
        for ((key, worker) in current.filterKeys { it !in keys }) {
            worker.lifetime.cancelAndJoin()
            inventoryStore.remove(key)
        }
        for (key in keys - retained.keys) {
            val lifetime = SupervisorJob(scope.coroutineContext[Job])
            val child = CoroutineScope(scope.coroutineContext + lifetime)
            retained[key] = Worker(
                lifetime,
                LibraryFolderRepository(
                    config,
                    key.tree,
                    scanGate,
                    inventoryStore,
                    source,
                    child,
                    reading,
                    enrich,
                ),
            )
        }
        workers.value = retained.toMap()
    }

    override suspend fun refresh() {
        val request = requests.withLock {
            refreshJob?.takeIf { it.isActive } ?: scope.async {
                refreshing.value = true
                try {
                    try {
                        reading.refresh()
                    } catch (_: CancellationException) {
                        currentCoroutineContext().ensureActive()
                    } catch (_: IOException) {
                        // An unavailable KOReader source does not block independently granted roots.
                    }
                    val active = selection.map { (selected, roots) ->
                        roots.takeIf { matches(selected, it.keys) }
                    }.filterNotNull().first()
                    active.values.forEach {
                        try {
                            it.repository.refresh()
                        } catch (_: CancellationException) {
                            currentCoroutineContext().ensureActive()
                        }
                    }
                } finally {
                    refreshing.value = false
                }
            }.also { refreshJob = it }
        }
        request.await()
    }
}

private fun mergeCatalogs(trees: List<String>, catalogs: List<LibraryCatalog>): LibraryCatalog {
    val folders = trees.zip(catalogs).map { (uri, catalog) ->
        LibraryFolder(uri, catalog.books.map { it.id }.toSet(), catalog.sync, catalog.loading)
    }
    return LibraryCatalog(
        books = catalogs.flatMap { it.books }.distinctBy { it.id },
        configured = folders.isNotEmpty(),
        loading = catalogs.any { it.loading },
        sync = ReadingSyncStatus(
            running = catalogs.any { it.sync.running },
            lastSuccessAt = catalogs.mapNotNull { it.sync.lastSuccessAt }.minOrNull(),
            error = catalogs.firstNotNullOfOrNull { it.sync.error },
            stale = catalogs.any { it.sync.stale },
        ),
        folders = folders,
    )
}
