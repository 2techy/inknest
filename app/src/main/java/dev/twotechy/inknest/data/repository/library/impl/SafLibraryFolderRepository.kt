package dev.twotechy.inknest.data.repository.library.impl

import android.database.sqlite.SQLiteException
import dev.twotechy.inknest.data.local.database.library.LibraryBookEntity
import dev.twotechy.inknest.data.local.database.library.LibraryFolderEntity
import dev.twotechy.inknest.data.local.database.library.LibraryFolderKey
import dev.twotechy.inknest.data.local.database.library.LibraryInventoryStore
import dev.twotechy.inknest.data.repository.library.LibraryCatalog
import dev.twotechy.inknest.data.repository.library.LibraryRepository
import dev.twotechy.inknest.data.repository.reading.ReadingRepository
import dev.twotechy.inknest.data.repository.reading.ReadingState
import dev.twotechy.inknest.data.repository.shared.ObservationRetry
import dev.twotechy.inknest.data.repository.shared.recoverObservation
import dev.twotechy.inknest.data.repository.sources.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.source.books.BookCatalogDataSource
import dev.twotechy.inknest.data.source.books.CatalogBook
import dev.twotechy.inknest.data.source.books.ReadingMetadataResult
import dev.twotechy.inknest.data.source.books.ResolvedBook
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingStatus
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.ReadingSyncStatus
import dev.twotechy.inknest.platform.tracing.OperationTracer
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class SafLibraryFolderRepository(
    private val config: ReadingSourceConfigRepository,
    private val tree: String,
    private val indexGate: Mutex,
    private val inventoryStore: LibraryInventoryStore,
    private val source: BookCatalogDataSource,
    private val scope: CoroutineScope,
    private val reading: ReadingRepository,
    private val tracer: OperationTracer = OperationTracer(),
    private val now: () -> Long = System::currentTimeMillis,
    private val readMetadata: suspend (
        ReadingSourceConfig,
        ResolvedBook,
        ReadingItem,
    ) -> ReadingMetadataResult,
) : LibraryRepository {
    private val key = config.config.map { it.key() }.distinctUntilChanged()
    private val running = MutableStateFlow<Pair<LibraryFolderKey, Any>?>(null)
    private val errors = MutableStateFlow<Map<LibraryFolderKey, ReadingSyncIssue>>(emptyMap())
    private val observationRetry = ObservationRetry()
    private val readErrors = MutableStateFlow<Set<LibraryFolderKey>>(emptySet())
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
            }.recoverObservation(
                observationRetry,
                onError = { readErrors.update { it + selected } },
                onSuccess = { readErrors.update { it - selected } },
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, Triple(null, emptyList<LibraryBookEntity>(), null))
    private val catalogErrors = combine(errors, readErrors) { writes, reads ->
        reads.associateWith { ReadingSyncIssue.Cache } + writes
    }
    private val readingStates = combine(reading.trackedItems, reading.reading) { history, current ->
        history + listOfNotNull((current as? ReadingState.Content)?.current)
    }
    override val catalog = combine(inventory, key, readingStates, running, catalogErrors) {
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
                    currentPage = status?.currentPage,
                    totalPages = status?.totalPages,
                    availability =
                        status?.availability
                            ?: BookAvailability.Available,
                    description = book.description,
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
        inventory.collect { (active, _, folder) -> resumeIncomplete(active, folder) }
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
            refresh(onlyIncomplete = true)
        }
    }

    override suspend fun refresh() = refresh(onlyIncomplete = false)

    private suspend fun refresh(onlyIncomplete: Boolean) {
        observationRetry.retry()
        config.retryRead()
        val selected = config.config.first()
        val active = selected.key() ?: return
        val request = requests.withLock {
            job?.takeIf { it.first == active && it.second.isActive }?.second
                ?: scope.async {
                    indexGate.withLock { indexLibrary(selected, active, onlyIncomplete) }
                }.also {
                    job =
                        active to it
                }
        }
        request.await()
    }

    private suspend fun indexLibrary(
        selected: ReadingSourceConfig,
        active: LibraryFolderKey,
        onlyIncomplete: Boolean,
    ): Unit = tracer.begin(OperationTracer.LIBRARY_INDEX).use {
        val token = Any()
        running.value = active to token
        try {
            // A queued resume may outlive the scan that completed this folder.
            if (!indexRequired(active, onlyIncomplete)) return
            tracer.begin(FIRST_METADATA_TRACE).use { firstMetadata ->
                val found = source.scanBooks(active.tree)
                replaceInventory(active, found)
                readAndSaveMetadata(selected, active, found, firstMetadata)
            }
            commit(active) {
                inventoryStore.putFolder(
                    LibraryFolderEntity(
                        active.source,
                        active.tree,
                        now(),
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

    private suspend fun indexRequired(active: LibraryFolderKey, onlyIncomplete: Boolean): Boolean =
        !onlyIncomplete || inventoryStore.observeFolder(active).first()?.complete != true

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
                LibraryFolderEntity(active.source, active.tree, now()),
                rows,
            )
        }
    }

    private suspend fun readAndSaveMetadata(
        selected: ReadingSourceConfig,
        active: LibraryFolderKey,
        found: List<CatalogBook>,
        firstMetadata: AutoCloseable,
    ) {
        for ((index, batch) in found.chunked(LIBRARY_METADATA_BATCH_SIZE).withIndex()) {
            tracer.begin(METADATA_BATCH_TRACE).use {
                val rows = batch.map { file -> readMetadata(selected, active, file) }
                saveMetadataBatch(active, rows)
            }
            if (index == 0) firstMetadata.close()
        }
    }

    private suspend fun saveMetadataBatch(active: LibraryFolderKey, rows: List<LibraryBookEntity>) =
        commit(active) { inventoryStore.putBooks(rows) }

    private suspend fun readMetadata(
        selected: ReadingSourceConfig,
        active: LibraryFolderKey,
        file: CatalogBook,
    ): LibraryBookEntity {
        currentCoroutineContext().ensureActive()
        val item = ReadingItem(
            ReadingItemId(active.source, file.path),
            file.relative.last().substringBeforeLast('.'),
            null,
            null,
        )
        val book = readMetadata(
            selected,
            ResolvedBook(file.path, active.tree, file.relative, file.document),
            item,
        )
        return LibraryBookEntity(
            active.source, active.tree, file.path,
            book.item.title, book.item.author, book.item.coverUri,
            book.metadata?.fingerprint, book.item.progress, book.item.status.name,
            description = book.metadata?.description,
        )
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

private const val LIBRARY_METADATA_BATCH_SIZE = 16
private const val FIRST_METADATA_TRACE = "InkNest.library.firstMetadata"
private const val METADATA_BATCH_TRACE = "InkNest.library.metadataBatch"

private fun Exception.libraryIssue(): ReadingSyncIssue? = when (this) {
    is SecurityException -> ReadingSyncIssue.PermissionLost
    is SQLiteException -> ReadingSyncIssue.Cache
    is IOException -> ReadingSyncIssue.Provider
    else -> null
}
