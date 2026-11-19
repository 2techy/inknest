package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.source.books.UnresolvedBookPathException
import dev.twotechy.inknest.data.source.formats.InvalidBookMetadataException
import dev.twotechy.inknest.data.source.formats.UnsupportedBookFormatException
import dev.twotechy.inknest.data.source.io.SourceLimitExceededException
import dev.twotechy.inknest.data.source.koreader.lua.LuaFailure
import dev.twotechy.inknest.data.source.koreader.lua.LuaSourceException
import dev.twotechy.inknest.data.source.probe.ProbeIssue
import dev.twotechy.inknest.data.source.probe.ProbeRead
import dev.twotechy.inknest.data.source.probe.readCapability
import dev.twotechy.inknest.data.source.saf.MissingSourceFileException
import dev.twotechy.inknest.data.source.statistics.InvalidStatisticsSnapshotException
import dev.twotechy.inknest.data.source.statistics.UnstableStatisticsSnapshotException
import dev.twotechy.inknest.data.source.statistics.UnsupportedStatisticsSchemaException
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
            MissingSourceFileException() to ProbeRead.Unavailable(ProbeIssue.MissingFile),
            UnresolvedBookPathException() to ProbeRead.Unavailable(ProbeIssue.UnresolvedPath),
            UnsupportedBookFormatException() to ProbeRead.Unavailable(ProbeIssue.Unsupported),
            UnsupportedStatisticsSchemaException() to
                ProbeRead.Unavailable(ProbeIssue.UnsupportedSchema),
            SecurityException() to ProbeRead.Failed(ProbeIssue.PermissionLost),
            SourceLimitExceededException() to ProbeRead.Failed(ProbeIssue.TooLarge),
            InvalidBookMetadataException() to ProbeRead.Failed(ProbeIssue.InvalidMetadata),
            InvalidStatisticsSnapshotException() to ProbeRead.Failed(ProbeIssue.InvalidSnapshot),
            UnstableStatisticsSnapshotException() to ProbeRead.Failed(ProbeIssue.SourceChanged),
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
