package dev.twotechy.inknest.data.source

import dev.twotechy.inknest.data.repository.ProbeIssue
import dev.twotechy.inknest.data.repository.ProbeRead
import dev.twotechy.inknest.data.repository.readCapability
import dev.twotechy.inknest.data.source.lua.HistoryEntryDto
import dev.twotechy.inknest.data.source.lua.ReaderLuaDataSource
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
import dev.twotechy.inknest.model.SourceAccessState
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class CurrentReadingSnapshot(
    val current: ReadingItem?,
    val warning: ReadingSyncIssue? = null,
    val history: List<ReadingItem>? = null,
    val metadata: Map<String, BookMetadata> = emptyMap(),
    val skippedHistoryEntries: Int = 0,
)
internal class ReadingRefreshException(val issue: ReadingSyncIssue, cause: Throwable? = null) :
    IOException(issue.name, cause)
internal fun interface CurrentReadingSource {
    suspend fun read(config: ReadingSourceConfig): CurrentReadingSnapshot
}

internal class KoreaderCurrentSource(
    private val files: SourceFileReader,
    private val books: BookProbeDataSource,
    private val lua: ReaderLuaDataSource = ReaderLuaDataSource(),
    private val access: SafSourceAccess? = null,
    private val metadataReader: BookMetadataReader? = null,
) : CurrentReadingSource {
    override suspend fun read(config: ReadingSourceConfig): CurrentReadingSnapshot =
        withContext(Dispatchers.IO) {
            val tree = checkNotNull(config.koreaderTreeUri)
            when (access?.inspect(tree)) {
                SourceAccessState.PermissionLost ->
                    throw ReadingRefreshException(ReadingSyncIssue.PermissionLost)

                null, SourceAccessState.Granted -> Unit

                else -> throw ReadingRefreshException(ReadingSyncIssue.Provider)
            }
            val parsed = readCapability {
                files.read(tree, listOf("settings.reader.lua"), lua::lastFile)
            }.result
            val current = parsed.validCurrentPath()
            val history = readCapability {
                files.read(tree, listOf("history.lua"), lua::trackedHistory)
            }.result
            val entries = (history as? ProbeRead.Success)?.value
            val path = (current as? ProbeRead.Success)?.value ?: run {
                ensureHistoryFallbackAllowed(current.issueOrNull())
                val fallback = entries?.entries ?: when (history) {
                    ProbeRead.Unavailable(ProbeIssue.MissingFile) -> {
                        if (current is ProbeRead.Success) {
                            emptyList()
                        } else {
                            throw ReadingRefreshException(ReadingSyncIssue.MissingData)
                        }
                    }

                    else -> throw ReadingRefreshException(
                        checkNotNull(history.issueOrNull()).syncIssue(),
                    )
                }
                if (fallback.isEmpty() && (entries?.skipped ?: 0) > 0) {
                    throw ReadingRefreshException(ReadingSyncIssue.InvalidData)
                }
                selectHistoryPath(fallback, current.issueOrNull() == ProbeIssue.InvalidLua)
            }
            val timestamps = linkedMapOf<String, Long>()
            var skipped = entries?.skipped ?: 0
            entries?.entries?.forEach { entry ->
                if (!entry.file.isSourcePath() || entry.time > Long.MAX_VALUE / 1000) {
                    skipped++
                } else {
                    timestamps.putIfAbsent(books.canonicalPath(entry.file), entry.time * 1000)
                }
            }
            val rawPaths = linkedMapOf<String, String>()
            if (path != null) rawPaths[books.canonicalPath(path)] = path
            entries?.entries?.filter { it.file.isSourcePath() }?.forEach {
                rawPaths.putIfAbsent(books.canonicalPath(it.file), it.file)
            }
            val items = linkedMapOf<String, ReadingItem>()
            val metadata = mutableMapOf<String, BookMetadata>()
            for ((canonical, raw) in rawPaths) {
                val resolved = readCapability { books.resolve(config, raw) }.result
                val fallback = ReadingItem(
                    id = ReadingItemId(checkNotNull(config.sourceId), canonical),
                    title = raw.substringAfterLast('/').substringBeforeLast('.').ifBlank { raw },
                    author = null,
                    progress = null,
                    availability = resolved.availability(),
                    lastReadAt = timestamps[canonical],
                )
                val enriched = if (resolved is ProbeRead.Success) {
                    metadataReader?.read(config, resolved.value, fallback)
                } else {
                    null
                }
                items[canonical] = enriched?.item ?: fallback
                enriched?.metadata?.let { metadata[canonical] = it }
            }
            val currentItem = path?.let { items[books.canonicalPath(it)] }
            val historyItems = when {
                entries != null && (timestamps.isNotEmpty() || skipped == 0) ->
                    timestamps.keys.mapNotNull { items[it] }

                entries != null -> null

                history == ProbeRead.Unavailable(ProbeIssue.MissingFile) -> emptyList()

                else -> null // A broken history document must not replace the last successful list.
            }
            val warning = when {
                skipped > 0 || historyItems == null -> ReadingSyncIssue.InvalidData
                else -> items.values.firstNotNullOfOrNull { it.availability.warning() }
            }
            CurrentReadingSnapshot(currentItem, warning, historyItems, metadata, skipped)
        }
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
