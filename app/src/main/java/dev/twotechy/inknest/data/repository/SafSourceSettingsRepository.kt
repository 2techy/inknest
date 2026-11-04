package dev.twotechy.inknest.data.repository

import android.net.Uri
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SafSourceAccess
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.model.SourceAccessState
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val reader: ReaderProbeRepository,
    private val files: SourceDocumentReader,
    scope: CoroutineScope,
) : SourceSettingsRepository {
    private data class Refresh(
        val running: Boolean = false,
        val updated: Long? = null,
        val generation: Long = 0,
    )
    private val refreshState = MutableStateFlow(Refresh())
    private val mutex = Mutex()
    override val state = combine(config.config, refreshState) { source, refresh ->
        source to refresh
    }
        .mapLatest { (source, refresh) ->
            val grant = access.inspect(source.koreaderTreeUri)
            SourceSettingsState(
                available = true,
                status = when (grant) {
                    SourceAccessState.NotSelected -> SourceStatus.NotConnected
                    SourceAccessState.Granted -> SourceStatus.Connected
                    SourceAccessState.PermissionLost -> SourceStatus.PermissionLost
                    else -> SourceStatus.Error
                },
                koreaderFolder = source.koreaderTreeUri,
                booksFolder = source.booksTreeUri,
                koreaderAccess = grant,
                booksAccess = access.inspect(source.booksTreeUri),
                sourcePackage = source.sourcePackage,
                refreshing = refresh.running,
                lastUpdated = refresh.updated,
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
                config.setTree(folder, uri)
            } catch (error: Exception) {
                if (uri !in
                    retainedBefore
                ) {
                    withContext(NonCancellable) { access.releaseUnused(uri, retainedBefore) }
                }
                throw error
            }
            val after = config.config.first()
            val retained = listOfNotNull(after.koreaderTreeUri, after.booksTreeUri).toSet()
            listOfNotNull(before.koreaderTreeUri, before.booksTreeUri).filterNot {
                it in retained
            }.forEach {
                access.releaseUnused(it, retained)
            }
            refreshState.update { Refresh(generation = it.generation + 1) }
        }

    override suspend fun disconnect() = mutex.withLock {
        val before = config.config.first()
        config.disconnect()
        listOfNotNull(before.koreaderTreeUri, before.booksTreeUri).distinct().forEach {
            access.releaseUnused(it, emptySet())
        }
        refreshState.update { Refresh(generation = it.generation + 1) }
    }

    override suspend fun refresh() = mutex.withLock {
        refreshState.update { it.copy(running = true) }
        try {
            val result = reader.probe(config.config.first())
            check(
                result.current.result is ProbeRead.Success &&
                    result.history.result is ProbeRead.Success,
            )
            refreshState.update { it.copy(updated = System.currentTimeMillis()) }
        } finally {
            refreshState.update { it.copy(running = false, generation = it.generation + 1) }
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

    override suspend fun clearCache(): Unit = error("Reading cache is not implemented")
}
