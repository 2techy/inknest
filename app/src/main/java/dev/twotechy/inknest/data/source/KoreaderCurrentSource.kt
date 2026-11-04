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

internal data class CurrentReadingSnapshot(
    val current: ReadingItem?,
    val warning: ReadingSyncIssue? = null,
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
) : CurrentReadingSource {
    override suspend fun read(config: ReadingSourceConfig): CurrentReadingSnapshot {
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
        val path = (current as? ProbeRead.Success)?.value ?: historyFallback(tree, current)
        if (path == null) return CurrentReadingSnapshot(null)
        val resolved = readCapability { books.resolve(config, path) }.result
        val availability = resolved.availability()
        val item = ReadingItem(
            id = ReadingItemId(checkNotNull(config.sourceId), books.canonicalPath(path)),
            title = path.substringAfterLast('/').substringBeforeLast('.').ifBlank { path },
            author = null,
            progress = null,
            availability = availability,
        )
        return CurrentReadingSnapshot(item, availability.warning())
    }

    private suspend fun historyFallback(tree: String, current: ProbeRead<String?>): String? {
        val currentIssue = current.issueOrNull()
        ensureHistoryFallbackAllowed(currentIssue)
        val history = readCapability {
            files.read(tree, listOf("history.lua"), lua::history)
        }.result
        return selectHistoryPath(
            history.historyEntries(current),
            currentIssue == ProbeIssue.InvalidLua,
        )
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

    ProbeRead.NotTested -> BookAvailability.Unresolved
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

private fun ProbeRead<List<HistoryEntryDto>>.historyEntries(
    current: ProbeRead<String?>,
): List<HistoryEntryDto> {
    if (this is ProbeRead.Success) return value
    if (this == ProbeRead.Unavailable(ProbeIssue.MissingFile) && current is ProbeRead.Success) {
        return emptyList()
    }
    throw ReadingRefreshException(checkNotNull(issueOrNull()).syncIssue())
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
