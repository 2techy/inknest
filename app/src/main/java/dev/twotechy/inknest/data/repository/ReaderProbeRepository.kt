package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.ResolvedBook
import dev.twotechy.inknest.data.source.SourceFileReader
import dev.twotechy.inknest.data.source.lua.HistoryEntryDto
import dev.twotechy.inknest.data.source.lua.ReaderLuaDataSource
import dev.twotechy.inknest.data.source.stats.StatisticsSnapshotProbe
import dev.twotechy.inknest.model.ReadingSourceConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ReaderProbeRepository(
    private val files: SourceFileReader,
    private val lua: ReaderLuaDataSource = ReaderLuaDataSource(),
    private val books: BookProbeDataSource? = null,
    private val stats: StatisticsSnapshotProbe? = null,
) {
    private val mutex = Mutex()

    suspend fun probe(config: ReadingSourceConfig): ReaderProbeResult = mutex.withLock {
        val current = readConfiguredCapability(config.koreaderTreeUri) { tree ->
            files.read(tree, listOf("settings.reader.lua"), lua::lastFile)
        }
        val history = readConfiguredCapability(config.koreaderTreeUri) { tree ->
            files.read(tree, listOf("history.lua"), lua::history)
        }
        val resolved = resolveCurrent(config, current.result)
        val book = (resolved.result as? ProbeRead.Success)?.value
        ReaderProbeResult(
            current = current,
            history = history,
            resolvedCurrent = resolved,
            resolvedHistory = resolveHistory(config, history.result),
            sidecar = withCurrentBook(book) { source, item -> source.sidecar(item) },
            epub = withCurrentBook(book) { source, item -> source.epub(item) },
            statistics = stats?.let { readConfiguredCapability(config.koreaderTreeUri, it::read) }
                ?: notTestedCapability(),
        )
    }

    suspend fun resolveForOpen(config: ReadingSourceConfig, sourcePath: String): ResolvedBook =
        checkNotNull(books).resolve(config, sourcePath)

    private suspend fun resolveCurrent(
        config: ReadingSourceConfig,
        current: ProbeRead<String?>,
    ): CapabilityResult<ResolvedBook?> {
        val source = books ?: return notTestedCapability()
        if (current !is ProbeRead.Success) return notTestedCapability()
        return readCapability { current.value?.let { source.resolve(config, it) } }
    }

    private suspend fun resolveHistory(
        config: ReadingSourceConfig,
        history: ProbeRead<List<HistoryEntryDto>>,
    ): List<CapabilityResult<ResolvedBook>> {
        val source = books ?: return emptyList()
        val entries = (history as? ProbeRead.Success)?.value.orEmpty()
        return entries.map { entry -> readCapability { source.resolve(config, entry.file) } }
    }

    private suspend fun <T> withCurrentBook(
        book: ResolvedBook?,
        read: suspend (BookProbeDataSource, ResolvedBook) -> T,
    ): CapabilityResult<T> {
        val source = books ?: return notTestedCapability()
        if (book == null) return notTestedCapability()
        return readCapability { read(source, book) }
    }
}
