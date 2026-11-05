package dev.twotechy.inknest.data.source

import dev.twotechy.inknest.data.source.stats.StatisticsSnapshotChanged
import dev.twotechy.inknest.data.source.stats.StatisticsSnapshotFiles
import dev.twotechy.inknest.data.source.stats.StatisticsSnapshotProbe
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StatisticsSnapshotFilesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun absentWalAndShmAreValidWhileDisappearingWalInvalidatesTheCopy() = runTest {
        val documents = Documents()
        val directory = temporary.newFolder()
        assertFalse(StatisticsSnapshotFiles(documents, "tree").copyTo(directory))
        assertEquals("db", File(directory, StatisticsSnapshotFiles.DATABASE_NAME).readText())
        documents.wal = true
        documents.loseWalOnOpen = true
        try {
            StatisticsSnapshotFiles(documents, "tree").copyTo(temporary.newFolder())
            fail("Never accept a copy missing the WAL observed before copying")
        } catch (_: StatisticsSnapshotChanged) { }
    }

    @Test fun providerRaceRetriesThreeTimesAndCleansUp() = runTest {
        val documents = Documents().apply { metadataFailure = true }
        val cache = temporary.newFolder()
        try {
            StatisticsSnapshotProbe(documents, cache).read("tree")
            fail("An unstable source must not publish a partial snapshot")
        } catch (failure: IOException) {
            assertEquals("Snapshot transport failed", failure.message)
            assertTrue(
                generateSequence<Throwable>(failure) {
                    it.cause
                }.any { it is IllegalArgumentException },
            )
        }
        assertEquals(3, documents.attempts)
        assertTrue(cache.listFiles().orEmpty().isEmpty())
    }

    private class Documents : SourceDocumentReader {
        var wal = false
        var loseWalOnOpen = false
        var metadataFailure = false
        var attempts = 0

        override suspend fun describe(tree: String, path: List<String>): SourceDocument {
            val name = path.last()
            if (name == StatisticsSnapshotFiles.DATABASE_NAME) attempts++
            checkWalMetadata(path)
            if (name.endsWith("-shm")) throw MissingSourceFile()
            if (name.endsWith("-wal") && !wal) throw MissingSourceFile()
            return SourceDocument("content://test/$name", null, 2, 1)
        }

        private fun checkWalMetadata(path: List<String>) {
            if (path.last().endsWith("-wal") && metadataFailure) {
                safProviderOperation { throw IllegalArgumentException("document vanished") }
            }
        }

        override suspend fun <T> read(
            tree: String,
            path: List<String>,
            parse: suspend (InputStream) -> T,
        ): T {
            val input = safProviderOperation {
                if (path.last().endsWith("-wal") && loseWalOnOpen) {
                    throw FileNotFoundException("WAL removed after metadata lookup")
                }
                ByteArrayInputStream("db".toByteArray())
            }
            return input.use { parse(it) }
        }
    }
}
