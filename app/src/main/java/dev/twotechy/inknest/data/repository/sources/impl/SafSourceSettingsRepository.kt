package dev.twotechy.inknest.data.repository.sources.impl

import android.provider.DocumentsContract
import androidx.core.net.toUri
import dev.twotechy.inknest.data.repository.library.LibraryRepository
import dev.twotechy.inknest.data.repository.reading.impl.RoomReadingRepository
import dev.twotechy.inknest.data.repository.sources.BookFolderConflictException
import dev.twotechy.inknest.data.repository.sources.BookFolderState
import dev.twotechy.inknest.data.repository.sources.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.repository.sources.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.sources.SourceSettingsState
import dev.twotechy.inknest.data.repository.sources.SourceStatus
import dev.twotechy.inknest.data.repository.sources.SourceTreeSelection
import dev.twotechy.inknest.data.repository.statistics.StatisticsRepository
import dev.twotechy.inknest.data.source.books.FileBookCoverStore
import dev.twotechy.inknest.data.source.saf.MissingSourceFileException
import dev.twotechy.inknest.data.source.saf.PrimaryTreePath
import dev.twotechy.inknest.data.source.saf.SafSourceAccess
import dev.twotechy.inknest.data.source.saf.SourceDocumentReader
import dev.twotechy.inknest.model.KOREADER_PACKAGE
import dev.twotechy.inknest.model.SourceAccessState
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
internal class SafSourceSettingsRepository(
    private val config: ReadingSourceConfigRepository,
    private val access: SafSourceAccess,
    private val reading: RoomReadingRepository,
    private val cacheFile: File,
    private val files: SourceDocumentReader,
    scope: CoroutineScope,
    private val library: LibraryRepository,
    private val covers: FileBookCoverStore,
    private val statistics: StatisticsRepository,
) : SourceSettingsRepository {
    private data class Refresh(val generation: Long = 0)
    private val refreshState = MutableStateFlow(Refresh())
    private val mutex = Mutex()
    override val state = combine(
        config.config,
        refreshState,
        reading.sync,
        library.catalog.map {
            it.sync
        }.distinctUntilChanged(),
        statistics.data.map { it.sync to it.updatedAt }.distinctUntilChanged(),
    ) {
            source,
            refresh,
            sync,
            catalogSync,
            stats,
        ->
        Triple(
            source,
            refresh,
            sync.copy(
                running = sync.running || catalogSync.running || stats.first.running,
                lastSuccessAt = listOfNotNull(
                    sync.lastSuccessAt,
                    catalogSync.lastSuccessAt,
                    stats.second,
                ).maxOrNull(),
            ),
        )
    }
        .mapLatest { (source, _, sync) ->
            val grant = access.inspect(source.koreaderTreeUri)
            val bookFolders = source.booksTreeUris.sorted().map {
                BookFolderState(it, access.inspect(it))
            }
            SourceSettingsState(
                available = true,
                status = sourceStatus(listOf(grant) + bookFolders.map { it.access }),
                koreaderFolder = source.koreaderTreeUri,
                booksFolders = bookFolders,
                koreaderAccess = grant,
                sourcePackage = source.sourcePackage,
                refreshing = sync.running,
                lastUpdated = sync.lastSuccessAt,
                cacheBytes = if (source.koreaderTreeUri == null &&
                    source.booksTreeUris.isEmpty()
                ) {
                    null
                } else {
                    withContext(Dispatchers.IO) {
                        listOf(
                            cacheFile,
                            File(cacheFile.path + "-wal"),
                            File(cacheFile.path + "-shm"),
                        ).sumOf {
                            it.length()
                        } + covers.sizeBytes()
                    }
                },
            )
        }.stateIn(
            scope,
            SharingStarted.WhileSubscribed(5_000),
            SourceSettingsState(available = true),
        )

    override suspend fun selectApp(packageName: String) {
        require(packageName == KOREADER_PACKAGE)
        config.setSourcePackage(packageName)
    }

    override suspend fun connectTree(selection: SourceTreeSelection) = mutex.withLock {
        val (folder, uri, flags, replacedTree) = selection
        val before = config.config.first()
        if (folder == SourceFolder.Books) validateBookTree(before.booksTreeUris, selection)
        try {
            access.persist(uri.toUri(), flags)
            if (folder == SourceFolder.KOReader) validateReaderTree(uri)
            val candidate = config.treeCandidate(before, folder, uri, replacedTree)
            val paired = if (folder == SourceFolder.KOReader && selection.pairKoreader) {
                candidate.copy(sourcePackage = before.sourcePackage ?: KOREADER_PACKAGE)
            } else {
                candidate
            }
            reading.replaceSource(before, paired)
        } finally {
            releaseUnused(before.grantedTrees + uri)
        }
        refreshState.update { Refresh(generation = it.generation + 1) }
    }

    override suspend fun removeBookTree(uri: String) = mutex.withLock {
        val before = config.config.first()
        if (uri !in before.booksTreeUris) return@withLock
        try {
            reading.replaceSource(before, before.copy(booksTreeUris = before.booksTreeUris - uri))
        } finally {
            releaseUnused(before.grantedTrees)
        }
        refreshState.update { Refresh(generation = it.generation + 1) }
    }

    private suspend fun releaseUnused(candidates: Set<String>) = withContext(NonCancellable) {
        val retained = config.config.first().grantedTrees
        candidates.forEach { access.releaseUnused(it, retained) }
    }

    private fun validateBookTree(trees: Set<String>, selection: SourceTreeSelection) {
        val uri = selection.uri
        val previous = selection.replacedTree
        if (previous != null) require(previous in trees)
        val candidate = PrimaryTreePath.parse(DocumentsContract.getTreeDocumentId(uri.toUri()))
            ?: throw IOException("Unsupported books folder")
        val existing = (trees - listOfNotNull(previous).toSet()).map {
            PrimaryTreePath.parse(DocumentsContract.getTreeDocumentId(it.toUri()))
                ?: throw IOException("Unsupported saved folder")
        }
        if (existing.any { candidate.overlaps(it) }) throw BookFolderConflictException()
    }

    override suspend fun disconnect() = mutex.withLock {
        val before = config.config.first()
        reading.disconnect()
        before.grantedTrees.forEach {
            access.releaseUnused(it, emptySet())
        }
        refreshState.update { Refresh(generation = it.generation + 1) }
    }

    override suspend fun refresh() {
        try {
            library.refresh()
        } finally {
            statistics.refresh()
            refreshState.update { Refresh(generation = it.generation + 1) }
        }
    }
    private suspend fun validateReaderTree(tree: String) {
        for (name in listOf("settings.reader.lua", "history.lua")) {
            try {
                files.describe(tree, listOf(name))
                return
            } catch (_: MissingSourceFileException) { }
        }
        throw IOException("Selected directory has no KOReader settings or history")
    }

    override suspend fun clearCache() = mutex.withLock {
        try {
            reading.clearCache()
        } finally {
            statistics.refresh()
        }
    }
}

private fun sourceStatus(grants: List<SourceAccessState>): SourceStatus = when {
    SourceAccessState.Granted in grants -> SourceStatus.Connected
    SourceAccessState.PermissionLost in grants -> SourceStatus.PermissionLost
    grants.all { it == SourceAccessState.NotSelected } -> SourceStatus.NotConnected
    else -> SourceStatus.Error
}
