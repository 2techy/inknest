package dev.twotechy.inknest.data.source.probe

import dev.twotechy.inknest.data.source.books.UnresolvedBookPathException
import dev.twotechy.inknest.data.source.formats.InvalidBookMetadataException
import dev.twotechy.inknest.data.source.formats.UnsupportedBookFormatException
import dev.twotechy.inknest.data.source.io.SourceLimitExceededException
import dev.twotechy.inknest.data.source.koreader.lua.LuaFailure
import dev.twotechy.inknest.data.source.koreader.lua.LuaSourceException
import dev.twotechy.inknest.data.source.saf.MissingSourceFileException
import dev.twotechy.inknest.data.source.statistics.InvalidStatisticsSnapshotException
import dev.twotechy.inknest.data.source.statistics.UnstableStatisticsSnapshotException
import dev.twotechy.inknest.data.source.statistics.UnsupportedStatisticsSchemaException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.xmlpull.v1.XmlPullParserException

internal suspend fun <T> readCapability(read: suspend () -> T): CapabilityResult<T> {
    val result = try {
        ProbeRead.Success(read())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        error.toProbeRead()
    }
    return CapabilityResult(result, System.currentTimeMillis())
}

private fun Exception.toProbeRead(): ProbeRead<Nothing> {
    val unavailable = unavailableIssue()
    if (unavailable != null) return ProbeRead.Unavailable(unavailable)
    return ProbeRead.Failed(failureIssue())
}

private fun Exception.unavailableIssue(): ProbeIssue? = when (this) {
    is MissingSourceFileException -> ProbeIssue.MissingFile
    is UnresolvedBookPathException -> ProbeIssue.UnresolvedPath
    is UnsupportedBookFormatException -> ProbeIssue.Unsupported
    is UnsupportedStatisticsSchemaException -> ProbeIssue.UnsupportedSchema
    else -> null
}

private fun Exception.failureIssue(): ProbeIssue = when (this) {
    is SecurityException -> ProbeIssue.PermissionLost
    is LuaSourceException -> reason.toProbeIssue()
    is SourceLimitExceededException -> ProbeIssue.TooLarge
    is XmlPullParserException, is InvalidBookMetadataException -> ProbeIssue.InvalidMetadata
    is InvalidStatisticsSnapshotException -> ProbeIssue.InvalidSnapshot
    is UnstableStatisticsSnapshotException -> ProbeIssue.SourceChanged
    is IOException -> ProbeIssue.ProviderError
    else -> throw this
}

private fun LuaFailure.toProbeIssue(): ProbeIssue = when (this) {
    LuaFailure.TooLarge -> ProbeIssue.TooLarge
    LuaFailure.InstructionLimit -> ProbeIssue.BudgetExceeded
    LuaFailure.InvalidData -> ProbeIssue.InvalidLua
}
