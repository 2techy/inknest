package dev.twotechy.inknest.data.source

import dev.twotechy.inknest.data.source.InvalidBookMetadata
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceLimitExceeded
import dev.twotechy.inknest.data.source.UnresolvedBookPath
import dev.twotechy.inknest.data.source.UnsupportedBookFormat
import dev.twotechy.inknest.data.source.lua.LuaFailure
import dev.twotechy.inknest.data.source.lua.LuaSourceException
import dev.twotechy.inknest.data.source.stats.InvalidStatisticsSnapshot
import dev.twotechy.inknest.data.source.stats.UnstableStatisticsSnapshot
import dev.twotechy.inknest.data.source.stats.UnsupportedStatisticsSchema
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
    is MissingSourceFile -> ProbeIssue.MissingFile
    is UnresolvedBookPath -> ProbeIssue.UnresolvedPath
    is UnsupportedBookFormat -> ProbeIssue.Unsupported
    is UnsupportedStatisticsSchema -> ProbeIssue.UnsupportedSchema
    else -> null
}

private fun Exception.failureIssue(): ProbeIssue = when (this) {
    is SecurityException -> ProbeIssue.PermissionLost
    is LuaSourceException -> reason.toProbeIssue()
    is SourceLimitExceeded -> ProbeIssue.TooLarge
    is XmlPullParserException, is InvalidBookMetadata -> ProbeIssue.InvalidMetadata
    is InvalidStatisticsSnapshot -> ProbeIssue.InvalidSnapshot
    is UnstableStatisticsSnapshot -> ProbeIssue.SourceChanged
    is IOException -> ProbeIssue.ProviderError
    else -> throw this
}

private fun LuaFailure.toProbeIssue(): ProbeIssue = when (this) {
    LuaFailure.TooLarge -> ProbeIssue.TooLarge
    LuaFailure.InstructionLimit -> ProbeIssue.BudgetExceeded
    LuaFailure.InvalidData -> ProbeIssue.InvalidLua
}
