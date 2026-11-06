package dev.twotechy.inknest.data.source

import dev.twotechy.inknest.data.source.ProbeIssue
import dev.twotechy.inknest.data.source.ProbeRead
import dev.twotechy.inknest.data.source.readCapability
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class SafProviderOperationTest {
    @Test fun providerArgumentFailureBecomesAnIoFailureWithoutParsingMessageText() = runTest {
        val original = IllegalArgumentException("OEM-specific provider failure")
        try {
            safProviderOperation<Unit> { throw original }
            fail("Provider exception must be normalized")
        } catch (normalized: IOException) {
            assertSame(original, normalized.cause)
        }
        val result = readCapability<Unit> { safProviderOperation { throw original } }
        assertEquals(ProbeRead.Failed(ProbeIssue.ProviderError), result.result)
    }

    @Test fun missingStreamRetainsCauseAndUsesMissingFileCapability() = runTest {
        val original = FileNotFoundException("settings.reader.lua replaced while opening")
        try {
            safProviderOperation<Unit> { throw original }
            fail("Missing provider files must retain their domain meaning")
        } catch (normalized: MissingSourceFile) {
            assertSame(original, normalized.cause)
        }
        val result = readCapability<Unit> { safProviderOperation { throw original } }
        assertEquals(ProbeRead.Unavailable(ProbeIssue.MissingFile), result.result)
    }

    @Test fun cancellationAccessErrorsAndUnexpectedProgrammingErrorsPropagateUnchanged() {
        for (original in listOf(
            CancellationException(),
            SecurityException(),
            IllegalStateException(),
        )) {
            try {
                safProviderOperation<Unit> { throw original }
                fail("Unrelated failures must not become IO errors")
            } catch (actual: Exception) {
                assertSame(original, actual)
            }
        }
    }
}
