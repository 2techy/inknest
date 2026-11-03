package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceFileReader
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ReaderProbeRepositoryTest {
    private val config = ReadingSourceConfig(koreaderTreeUri = "tree")
    private class Files(val content: (String) -> String) : SourceFileReader {
        override suspend fun <T> read(
            tree: String,
            path: List<String>,
            parse: suspend (InputStream) -> T,
        ): T = content(path.single()).byteInputStream().use { parse(it) }
    }

    @Test fun `missing current and empty history are different results`() = runTest {
        val repo = ReaderProbeRepository(
            Files {
                if (it == "settings.reader.lua") throw MissingSourceFile()
                "return {}"
            },
        )
        val result = repo.probe(config)
        assertEquals(ProbeRead.Failed(ProbeIssue.MissingFile), result.current.result)
        assertEquals(ProbeRead.Success(emptyList<Any>()), result.history.result)
        assertTrue(result.current.checkedAt > 0)
        assertTrue(result.history.checkedAt >= result.current.checkedAt)
    }

    @Test fun `bad settings do not suppress independently valid history`() = runTest {
        val result = ReaderProbeRepository(
            Files {
                if (it == "settings.reader.lua") {
                    "return false"
                } else {
                    "return {{file='a.epub',time=2}}"
                }
            },
        ).probe(config)
        assertEquals(ProbeRead.Failed(ProbeIssue.InvalidLua), result.current.result)
        assertEquals(1, (result.history.result as ProbeRead.Success).value.size)
    }

    @Test fun `provider and permission errors never become empty success`() = runTest {
        for ((error, expected) in listOf(
            IOException() to ProbeIssue.ProviderError,
            SecurityException() to ProbeIssue.PermissionLost,
        )) {
            val result = ReaderProbeRepository(Files { throw error }).probe(config)
            assertEquals(ProbeRead.Failed(expected), result.current.result)
            assertEquals(ProbeRead.Failed(expected), result.history.result)
        }
    }

    @Test fun `cancelled read releases coordinator for a later run`() = runTest {
        var cancel = true
        val repo = ReaderProbeRepository(
            Files {
                if (cancel) throw CancellationException()
                "return {}"
            },
        )
        try {
            repo.probe(config)
            fail("Cancellation must propagate")
        } catch (
            _: CancellationException,
        ) { }
        cancel = false
        val result = repo.probe(config)
        assertEquals(ProbeRead.Success<String?>(null), result.current.result)
        assertEquals(ProbeRead.Success(emptyList<Any>()), result.history.result)
    }
}
