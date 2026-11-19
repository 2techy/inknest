package dev.twotechy.inknest.data.source

import dev.twotechy.inknest.data.source.saf.MissingSourceFile
import dev.twotechy.inknest.data.source.saf.SourceDocument
import dev.twotechy.inknest.data.source.saf.SourceDocumentReader
import dev.twotechy.inknest.data.source.saf.safProviderOperation
import dev.twotechy.inknest.data.source.stats.SnapshotFileMetadata
import dev.twotechy.inknest.data.source.stats.SnapshotFingerprint
import dev.twotechy.inknest.data.source.stats.StatisticsSnapshotChanged
import dev.twotechy.inknest.data.source.stats.impl.StatisticsSnapshotFiles
import dev.twotechy.inknest.data.source.stats.impl.StatisticsSnapshotProbe
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

    @Test fun fingerprintsDistinguishAbsentUnknownAndChangingWal() = runTest {
        val documents = Documents()
        val source = StatisticsSnapshotFiles(documents, "tree")
        val before = source.fingerprint()
        assertEquals(SnapshotFileMetadata.Present(2, 1), before.database)
        assertEquals(SnapshotFileMetadata.Absent, before.wal)
        assertEquals(SnapshotFileMetadata.Absent, before.shm)
        assertEquals(before, source.copyTo(temporary.newFolder()))
        documents.wal = true
        assertTrue(before != source.fingerprint())
        documents.modified = null
        assertEquals(SnapshotFileMetadata.Unknown, source.fingerprint().database)
        documents.modified = 0
        assertEquals(SnapshotFileMetadata.Unknown, source.fingerprint().wal)
        documents.modified = 1
        documents.size = null
        assertEquals(SnapshotFileMetadata.Unknown, source.fingerprint().database)
    }

    @Test fun onlyKnownSettledMetadataIsReusable() {
        fun fingerprint(file: SnapshotFileMetadata) = SnapshotFingerprint(
            file,
            SnapshotFileMetadata.Absent,
            SnapshotFileMetadata.Absent,
        )
        assertFalse(fingerprint(SnapshotFileMetadata.Unknown).reusableAt(10_000))
        assertFalse(fingerprint(SnapshotFileMetadata.Absent).reusableAt(10_000))
        assertFalse(fingerprint(SnapshotFileMetadata.Present(2, 5_001)).reusableAt(10_000))
        assertFalse(fingerprint(SnapshotFileMetadata.Present(2, 20_000)).reusableAt(10_000))
        assertTrue(fingerprint(SnapshotFileMetadata.Present(2, 5_000)).reusableAt(10_000))
    }

    @Test fun absentWalAndShmAreValidWhileDisappearingWalInvalidatesTheCopy() = runTest {
        val documents = Documents()
        val directory = temporary.newFolder()
        assertFalse(StatisticsSnapshotFiles(documents, "tree").copyTo(directory).hasWal)
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
        var modified: Long? = 1
        var size: Long? = 2

        override suspend fun describe(tree: String, path: List<String>): SourceDocument {
            val name = path.last()
            if (name == StatisticsSnapshotFiles.DATABASE_NAME) attempts++
            checkWalMetadata(path)
            if (name.endsWith("-shm")) throw MissingSourceFile()
            if (name.endsWith("-wal") && !wal) throw MissingSourceFile()
            return SourceDocument("content://test/$name", null, size, modified)
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
