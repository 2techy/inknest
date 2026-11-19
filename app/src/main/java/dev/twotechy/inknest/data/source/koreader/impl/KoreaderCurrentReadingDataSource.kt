package dev.twotechy.inknest.data.source.koreader.impl

import dev.twotechy.inknest.data.source.books.BookMetadata
import dev.twotechy.inknest.data.source.books.BookMetadataReader
import dev.twotechy.inknest.data.source.books.ResolvedBook
import dev.twotechy.inknest.data.source.books.impl.SafBookDataSource
import dev.twotechy.inknest.data.source.koreader.CurrentReadingDataSource
import dev.twotechy.inknest.data.source.koreader.CurrentReadingSnapshot
import dev.twotechy.inknest.data.source.koreader.ReadingRefreshException
import dev.twotechy.inknest.data.source.koreader.lua.HistoryEntryDto
import dev.twotechy.inknest.data.source.koreader.lua.KoreaderLuaParser
import dev.twotechy.inknest.data.source.koreader.lua.TrackedHistoryDto
import dev.twotechy.inknest.data.source.probe.ProbeIssue
import dev.twotechy.inknest.data.source.probe.ProbeRead
import dev.twotechy.inknest.data.source.probe.readCapability
import dev.twotechy.inknest.data.source.saf.SafSourceAccess
import dev.twotechy.inknest.data.source.saf.SourceDocumentReader
import dev.twotechy.inknest.data.source.saf.SourceFileReader
import dev.twotechy.inknest.data.source.saf.SourceMetadataCapture
import dev.twotechy.inknest.data.source.saf.SourceMetadataFingerprint
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.SourceAccessState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class KoreaderCurrentReadingDataSource(
    private val files: SourceFileReader,
    private val books: SafBookDataSource,
    private val lua: KoreaderLuaParser = KoreaderLuaParser(),
    private val access: SafSourceAccess? = null,
    private val metadataReader: BookMetadataReader? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
) : CurrentReadingDataSource {
    override suspend fun unchanged(
        config: ReadingSourceConfig,
        fingerprint: SourceMetadataFingerprint,
    ): Boolean = withContext(ioDispatcher) {
        val reader = files as? SourceDocumentReader ?: return@withContext false
        val grants = access ?: return@withContext false
        for (tree in config.grantedTrees) {
            if (grants.inspect(tree) != SourceAccessState.Granted) return@withContext false
        }
        fingerprint.matches(reader, now())
    }

    override suspend fun read(config: ReadingSourceConfig): CurrentReadingSnapshot {
        val capture = SourceMetadataCapture(now())
        return withContext(ioDispatcher + capture) {
            readSnapshot(config).copy(fingerprint = capture.fingerprint())
        }
    }

    private suspend fun readSnapshot(config: ReadingSourceConfig): CurrentReadingSnapshot {
        val input = readInput(config)
        val index = indexHistory(input.entries)
        val rows = readItems(config, readingPaths(input), index.timestamps)
        val history = trackedHistory(input, index, rows.items)
        val warning = when {
            index.skipped > 0 || history == null -> ReadingSyncIssue.InvalidData
            else -> rows.items.values.firstNotNullOfOrNull { it.availability.warning() }
        }
        return CurrentReadingSnapshot(
            input.path?.let { rows.items[books.canonicalPath(it)] },
            warning,
            history,
            rows.metadata,
            index.skipped,
        )
    }

    private suspend fun readInput(config: ReadingSourceConfig): ReadingInput {
        val tree = checkNotNull(config.koreaderTreeUri)
        when (access?.inspect(tree)) {
            SourceAccessState.PermissionLost ->
                throw ReadingRefreshException(ReadingSyncIssue.PermissionLost)

            null, SourceAccessState.Granted -> Unit

            else -> throw ReadingRefreshException(ReadingSyncIssue.Provider)
        }
        val current = readCapability {
            files.read(tree, listOf("settings.reader.lua"), lua::lastFile)
        }.result.validCurrentPath()
        val history = readCapability {
            files.read(tree, listOf("history.lua"), lua::trackedHistory)
        }.result
        return ReadingInput(currentPath(current, history), history)
    }

    private fun currentPath(
        current: ProbeRead<String?>,
        history: ProbeRead<TrackedHistoryDto>,
    ): String? {
        (current as? ProbeRead.Success)?.value?.let { return it }
        ensureHistoryFallbackAllowed(current.issueOrNull())
        val entries = (history as? ProbeRead.Success)?.value
        val fallback = entries?.entries ?: missingHistoryFallback(current, history)
        if (fallback.isEmpty() && (entries?.skipped ?: 0) > 0) {
            throw ReadingRefreshException(ReadingSyncIssue.InvalidData)
        }
        return selectHistoryPath(fallback, current.issueOrNull() == ProbeIssue.InvalidLua)
    }

    private fun missingHistoryFallback(
        current: ProbeRead<String?>,
        history: ProbeRead<TrackedHistoryDto>,
    ): List<HistoryEntryDto> {
        if (history != ProbeRead.Unavailable(ProbeIssue.MissingFile)) {
            throw ReadingRefreshException(checkNotNull(history.issueOrNull()).syncIssue())
        }
        if (current !is ProbeRead.Success) {
            throw ReadingRefreshException(ReadingSyncIssue.MissingData)
        }
        return emptyList()
    }

    private suspend fun indexHistory(entries: TrackedHistoryDto?): HistoryIndex {
        val timestamps = linkedMapOf<String, Long>()
        var skipped = entries?.skipped ?: 0
        for (entry in entries?.entries.orEmpty()) {
            if (!entry.file.isSourcePath() || entry.time > Long.MAX_VALUE / 1000) {
                skipped++
            } else {
                timestamps.putIfAbsent(books.canonicalPath(entry.file), entry.time * 1000)
            }
        }
        return HistoryIndex(timestamps, skipped)
    }

    private suspend fun readingPaths(input: ReadingInput): Map<String, String> {
        val paths = linkedMapOf<String, String>()
        input.path?.let { paths[books.canonicalPath(it)] = it }
        input.entries?.entries?.filter { it.file.isSourcePath() }?.forEach {
            paths.putIfAbsent(books.canonicalPath(it.file), it.file)
        }
        return paths
    }

    private suspend fun readItems(
        config: ReadingSourceConfig,
        paths: Map<String, String>,
        timestamps: Map<String, Long>,
    ): ReadingItems {
        val items = linkedMapOf<String, ReadingItem>()
        val metadata = mutableMapOf<String, BookMetadata>()
        for ((canonical, raw) in paths) {
            val resolved = readCapability { books.resolve(config, raw) }.result
            val fallback = ReadingItem(
                id = ReadingItemId(checkNotNull(config.sourceId), canonical),
                title = raw.substringAfterLast('/').substringBeforeLast('.').ifBlank { raw },
                author = null,
                progress = null,
                availability = resolved.availability(),
                lastReadAt = timestamps[canonical],
            )
            val metadataResult = if (resolved is ProbeRead.Success) {
                metadataReader?.readReadingMetadata(config, resolved.value, fallback)
            } else {
                null
            }
            items[canonical] = metadataResult?.item ?: fallback
            metadataResult?.metadata?.let { metadata[canonical] = it }
        }
        return ReadingItems(items, metadata)
    }

    private fun trackedHistory(
        input: ReadingInput,
        index: HistoryIndex,
        items: Map<String, ReadingItem>,
    ): List<ReadingItem>? = when {
        input.entries != null && (index.timestamps.isNotEmpty() || index.skipped == 0) ->
            index.timestamps.keys.mapNotNull { items[it] }

        input.entries != null -> null

        input.history == ProbeRead.Unavailable(ProbeIssue.MissingFile) -> emptyList()

        else -> null // A broken history document must not replace the last successful list.
    }

    private data class ReadingInput(val path: String?, val history: ProbeRead<TrackedHistoryDto>) {
        val entries: TrackedHistoryDto? get() = (history as? ProbeRead.Success)?.value
    }
    private data class HistoryIndex(val timestamps: Map<String, Long>, val skipped: Int)
    private data class ReadingItems(
        val items: Map<String, ReadingItem>,
        val metadata: Map<String, BookMetadata>,
    )
}

private fun ProbeRead<*>.issueOrNull(): ProbeIssue? = when (this) {
    is ProbeRead.Unavailable -> issue
    is ProbeRead.Failed -> issue
    else -> null
}

private fun ProbeRead<ResolvedBook>.availability(): BookAvailability = when (this) {
    is ProbeRead.Success -> BookAvailability.Available

    is ProbeRead.Unavailable -> if (issue == ProbeIssue.MissingFile) {
        BookAvailability.Missing
    } else {
        BookAvailability.Unresolved
    }

    is ProbeRead.Failed -> if (issue == ProbeIssue.PermissionLost) {
        BookAvailability.PermissionLost
    } else {
        BookAvailability.Unresolved
    }
}

private fun BookAvailability.warning(): ReadingSyncIssue? = when (this) {
    BookAvailability.Available -> null
    BookAvailability.PermissionLost -> ReadingSyncIssue.PermissionLost
    BookAvailability.Missing, BookAvailability.Unresolved -> ReadingSyncIssue.MissingData
}

private fun ProbeIssue.syncIssue(): ReadingSyncIssue = when (this) {
    ProbeIssue.PermissionLost -> ReadingSyncIssue.PermissionLost

    ProbeIssue.MissingFile, ProbeIssue.UnresolvedPath, ProbeIssue.NotConfigured ->
        ReadingSyncIssue.MissingData

    ProbeIssue.InvalidLua, ProbeIssue.TooLarge, ProbeIssue.BudgetExceeded ->
        ReadingSyncIssue.InvalidData

    else -> ReadingSyncIssue.Provider
}

private fun String.isSourcePath(): Boolean = startsWith('/') &&
    removePrefix("/").split('/').none { it.isEmpty() || it == "." || it == ".." }

private fun ProbeRead<String?>.validCurrentPath(): ProbeRead<String?> {
    val value = (this as? ProbeRead.Success)?.value ?: return this
    return if (value.isSourcePath()) this else ProbeRead.Failed(ProbeIssue.InvalidLua)
}

private fun ensureHistoryFallbackAllowed(issue: ProbeIssue?) {
    if (issue == null) return
    if (issue !in setOf(ProbeIssue.MissingFile, ProbeIssue.InvalidLua)) {
        throw ReadingRefreshException(issue.syncIssue())
    }
}

private fun selectHistoryPath(entries: List<HistoryEntryDto>, invalidCurrent: Boolean): String? {
    entries.firstOrNull { it.file.isSourcePath() }?.let { return it.file }
    if (entries.isNotEmpty() ||
        invalidCurrent
    ) {
        throw ReadingRefreshException(ReadingSyncIssue.InvalidData)
    }
    return null
}
