package dev.twotechy.inknest.data.repository

import android.net.Uri
import dev.twotechy.inknest.data.source.BookCoverCache
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SafSourceAccess
import dev.twotechy.inknest.data.source.SourceDocumentReader
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
    private val reading: CachedReadingRepository,
    private val cacheFile: File,
    private val files: SourceDocumentReader,
    scope: CoroutineScope,
    private val library: LibraryRepository,
    private val covers: BookCoverCache,
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
            val booksGrant = access.inspect(source.booksTreeUri)
            SourceSettingsState(
                available = true,
                status = sourceStatus(listOf(grant, booksGrant)),
                koreaderFolder = source.koreaderTreeUri,
                booksFolder = source.booksTreeUri,
                koreaderAccess = grant,
                booksAccess = booksGrant,
                sourcePackage = source.sourcePackage,
                refreshing = sync.running,
                lastUpdated = sync.lastSuccessAt,
                cacheBytes = if (source.koreaderTreeUri == null &&
                    source.booksTreeUri == null
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
        require(packageName == "org.koreader.launcher")
        config.setSourcePackage(packageName)
    }

    override suspend fun connectTree(folder: SourceFolder, uri: String, flags: Int) =
        mutex.withLock {
            val before = config.config.first()
            val retainedBefore = listOfNotNull(before.koreaderTreeUri, before.booksTreeUri).toSet()
            try {
                access.persist(Uri.parse(uri), flags)
                if (folder == SourceFolder.KOReader) validateReaderTree(uri)
                reading.replaceSource(before, config.treeCandidate(before, folder, uri))
            } finally {
                // DataStore may commit before the caller observes cancellation. Retain grants
                // referenced by the actual configuration, including a newly activated candidate.
                withContext(NonCancellable) {
                    val saved = config.config.first()
                    val retained = listOfNotNull(saved.koreaderTreeUri, saved.booksTreeUri).toSet()
                    (retainedBefore + uri).forEach { access.releaseUnused(it, retained) }
                }
            }
            refreshState.update { Refresh(generation = it.generation + 1) }
        }

    override suspend fun disconnect() = mutex.withLock {
        val before = config.config.first()
        reading.disconnect()
        listOfNotNull(before.koreaderTreeUri, before.booksTreeUri).distinct().forEach {
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
            } catch (_: MissingSourceFile) { }
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
