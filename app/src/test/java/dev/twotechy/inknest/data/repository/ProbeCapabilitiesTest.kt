package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.source.InvalidBookMetadata
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.ProbeIssue
import dev.twotechy.inknest.data.source.ProbeRead
import dev.twotechy.inknest.data.source.SourceLimitExceeded
import dev.twotechy.inknest.data.source.UnresolvedBookPath
import dev.twotechy.inknest.data.source.UnsupportedBookFormat
import dev.twotechy.inknest.data.source.lua.LuaFailure
import dev.twotechy.inknest.data.source.lua.LuaSourceException
import dev.twotechy.inknest.data.source.readCapability
import dev.twotechy.inknest.data.source.stats.InvalidStatisticsSnapshot
import dev.twotechy.inknest.data.source.stats.UnstableStatisticsSnapshot
import dev.twotechy.inknest.data.source.stats.UnsupportedStatisticsSchema
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ProbeCapabilitiesTest {
    @Test fun `expected source failures retain their capability classification`() = runTest {
        val cases = listOf(
            MissingSourceFile() to ProbeRead.Unavailable(ProbeIssue.MissingFile),
            UnresolvedBookPath() to ProbeRead.Unavailable(ProbeIssue.UnresolvedPath),
            UnsupportedBookFormat() to ProbeRead.Unavailable(ProbeIssue.Unsupported),
            UnsupportedStatisticsSchema() to ProbeRead.Unavailable(ProbeIssue.UnsupportedSchema),
            SecurityException() to ProbeRead.Failed(ProbeIssue.PermissionLost),
            SourceLimitExceeded() to ProbeRead.Failed(ProbeIssue.TooLarge),
            InvalidBookMetadata() to ProbeRead.Failed(ProbeIssue.InvalidMetadata),
            InvalidStatisticsSnapshot() to ProbeRead.Failed(ProbeIssue.InvalidSnapshot),
            UnstableStatisticsSnapshot() to ProbeRead.Failed(ProbeIssue.SourceChanged),
            IOException() to ProbeRead.Failed(ProbeIssue.ProviderError),
            LuaSourceException(LuaFailure.TooLarge) to ProbeRead.Failed(ProbeIssue.TooLarge),
            LuaSourceException(LuaFailure.InstructionLimit) to
                ProbeRead.Failed(ProbeIssue.BudgetExceeded),
            LuaSourceException(LuaFailure.InvalidData) to ProbeRead.Failed(ProbeIssue.InvalidLua),
        )
        for ((error, expected) in cases) {
            val result = readCapability<Nothing> { throw error }
            assertEquals(expected, result.result)
            assertTrue(result.checkedAt > 0)
        }
    }

    @Test fun `cancellation and programming errors propagate unchanged`() = runTest {
        for (error in listOf(CancellationException(), IllegalStateException())) {
            try {
                readCapability<Nothing> { throw error }
                fail("Unexpected errors must not become a source failure")
            } catch (actual: Exception) {
                assertSame(error, actual)
            }
        }
    }
}
