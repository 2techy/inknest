package dev.twotechy.inknest.data.repository

import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.local.reading.LibraryBookEntity
import dev.twotechy.inknest.data.local.reading.LibraryFolderEntity
import dev.twotechy.inknest.data.local.reading.LibraryFolderKey
import dev.twotechy.inknest.data.local.reading.LibraryInventoryStore
import dev.twotechy.inknest.data.source.BookCatalogSource
import dev.twotechy.inknest.data.source.CatalogBook
import dev.twotechy.inknest.data.source.EnrichedReadingItem
import dev.twotechy.inknest.data.source.ResolvedBook
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingStatus
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class LibraryFolderRepository(
    private val config: ReadingSourceConfigRepository,
    private val tree: String,
    private val scanGate: Mutex,
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
    private val key = config.config.map { it.key() }.distinctUntilChanged()
    private val running = MutableStateFlow<Pair<LibraryFolderKey, Any>?>(null)
    private val errors = MutableStateFlow<Map<LibraryFolderKey, ReadingSyncIssue>>(emptyMap())
    private val observedFolders = mutableSetOf<LibraryFolderKey>()
    private val requests = Mutex()
    private val writes = Mutex()
    private var job: Pair<LibraryFolderKey, Deferred<Unit>>? = null
    private val inventory = key.flatMapLatest { selected ->
        if (selected == null) {
            flowOf(Triple(null, emptyList<LibraryBookEntity>(), null))
        } else {
            combine(
                inventoryStore.observe(selected),
                inventoryStore.observeFolder(selected),
            ) {
                    books,
                    folder,
                ->
                Triple(selected, books, folder)
            }
        }
    }
    private val readingStates = combine(reading.trackedItems, reading.reading) { history, current ->
        history + listOfNotNull((current as? ReadingState.Content)?.current)
    }
    override val catalog = combine(inventory, key, readingStates, running, errors) {
            inventory,
            active,
            tracked,
            running,
            errors,
        ->
        if (active == null) {
            LibraryCatalog()
        } else {
            val books = inventory.second.takeIf { inventory.first == active }.orEmpty()
            val statuses = tracked.associateBy { it.id }
            val items = books.map { book ->
                val id = ReadingItemId(book.sourceId, book.path)
                val status = statuses[id]
                ReadingItem(
                    id,
                    book.title,
                    book.author,
                    status?.progress ?: book.progress,
                    book.coverUri,
                    status =
                        status?.status
                            ?: ReadingStatus.valueOf(book.status),
                    lastReadAt = status?.lastReadAt,
                )
            }
            val updated = inventory.third?.takeIf { inventory.first == active }?.updatedAt
            LibraryCatalog(
                items,
                true,
                updated == null && active !in errors,
                ReadingSyncStatus(
                    lastSuccessAt = updated,
                    running = running?.first == active,
                    error = errors[active],
                    stale = errors[active] != null && updated != null,
                ),
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, LibraryCatalog(loading = true))

    init {
        observeSelection()
        observeIncompleteFolders()
    }

    private fun observeSelection() = scope.launch {
        key.collect { active ->
            requests.withLock {
                if (config.config.first().key() != active) return@withLock
                if (job?.first != active) job?.second?.cancel()
            }
        }
    }

    private fun observeIncompleteFolders() = scope.launch {
        key.flatMapLatest { active ->
            if (active == null) {
                flowOf(null to null)
            } else {
                inventoryStore.observeFolder(active).map { active to it }
            }
        }.collect { (active, folder) -> resumeIncomplete(active, folder) }
    }

    private suspend fun resumeIncomplete(active: LibraryFolderKey?, folder: LibraryFolderEntity?) {
        if (active == null) return
        val cleared = if (folder == null) {
            observedFolders.remove(active)
        } else {
            observedFolders.add(active)
            false
        }
        if (folder?.complete == true) return
        if (running.value?.first == active) {
            if (!cleared) return
            // Clear cache invalidates an in-flight discovery, not just its saved rows.
            requests.withLock { job?.takeIf { it.first == active }?.second?.cancel() }
        }
        scope.launch {
            if (config.config.first().key() != active) return@launch
            if (inventoryStore.observeFolder(active).first()?.complete != true) refresh()
        }
    }

    override suspend fun refresh() {
        val selected = config.config.first()
        val active = selected.key() ?: return
        val request = requests.withLock {
            job?.takeIf { it.first == active && it.second.isActive }?.second
                ?: scope.async { scanGate.withLock { scan(selected, active) } }.also {
                    job =
                        active to it
                }
        }
        request.await()
    }

    private suspend fun scan(selected: ReadingSourceConfig, active: LibraryFolderKey) {
        val token = Any()
        running.value = active to token
        try {
            val found = source.scan(active.tree)
            replaceInventory(active, found)
            enrichBooks(selected, active, found)
            commit(active) {
                inventoryStore.putFolder(
                    LibraryFolderEntity(
                        active.source,
                        active.tree,
                        System.currentTimeMillis(),
                        true,
                    ),
                )
            }
            errors.value = errors.value - active
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val issue = error.libraryIssue() ?: throw error
            errors.value = errors.value + (active to issue)
        } finally {
            if (running.value?.second === token) running.value = null
        }
    }

    private suspend fun replaceInventory(active: LibraryFolderKey, found: List<CatalogBook>) {
        val old = inventoryStore.books(active).associateBy { it.path }
        val rows = found.map { file ->
            old[file.path] ?: LibraryBookEntity(
                active.source,
                active.tree,
                file.path,
                file.relative.last().substringBeforeLast('.'),
            )
        }
        commit(active) {
            inventoryStore.replace(
                LibraryFolderEntity(active.source, active.tree, System.currentTimeMillis()),
                rows,
            )
        }
    }

    private suspend fun enrichBooks(
        selected: ReadingSourceConfig,
        active: LibraryFolderKey,
        found: List<CatalogBook>,
    ) {
        for (batch in found.chunked(4)) {
            val rows = batch.map { file ->
                val item = ReadingItem(
                    ReadingItemId(active.source, file.path),
                    file.relative.last().substringBeforeLast('.'),
                    null,
                    null,
                )
                val book = enrich(
                    selected,
                    ResolvedBook(file.path, active.tree, file.relative, file.document),
                    item,
                )
                LibraryBookEntity(
                    active.source, active.tree, file.path,
                    book.item.title, book.item.author, book.item.coverUri,
                    book.metadata?.fingerprint, book.item.progress, book.item.status.name,
                )
            }
            commit(active) { inventoryStore.putBooks(rows) }
        }
    }

    private suspend fun commit(active: LibraryFolderKey, action: suspend () -> Unit) =
        writes.withLock {
            currentCoroutineContext().ensureActive()
            if (config.config.first().key() !=
                active
            ) {
                throw CancellationException("Books folder changed")
            }
            action()
        }
    private fun ReadingSourceConfig.key(): LibraryFolderKey? {
        val source = sourceId ?: return null
        if (tree !in booksTreeUris) return null
        return LibraryFolderKey(source, tree)
    }
}

private fun Exception.libraryIssue(): ReadingSyncIssue? = when (this) {
    is SecurityException -> ReadingSyncIssue.PermissionLost
    is SQLiteException -> ReadingSyncIssue.Cache
    is IOException -> ReadingSyncIssue.Provider
    else -> null
}
