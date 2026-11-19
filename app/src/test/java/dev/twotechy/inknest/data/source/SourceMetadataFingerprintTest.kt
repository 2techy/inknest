package dev.twotechy.inknest.data.source

import dev.twotechy.inknest.data.source.saf.MissingSourceFile
import dev.twotechy.inknest.data.source.saf.SourceDocument
import dev.twotechy.inknest.data.source.saf.SourceDocumentReader
import dev.twotechy.inknest.data.source.saf.SourceMetadataCapture
import dev.twotechy.inknest.data.source.saf.SourceMetadataFingerprint
import dev.twotechy.inknest.data.source.saf.SourceMetadataFingerprint.Document
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SourceMetadataFingerprintTest {
    private val book = Document("tree", listOf("book.epub"))
    private val sidecar = Document("tree", listOf("book.sdr", "metadata.epub.lua"))
    private val metadata = SourceDocument("content://fixture/book", null, 2, 1)

    @Test fun oversizedCaptureDisablesReuseInsteadOfRetainingAnUnboundedHistory() {
        val capture = SourceMetadataCapture(10_000)
        repeat(10_000) {
            capture.record(Document("tree", listOf("book-$it")), metadata)
        }
        assertNull(capture.fingerprint())
    }

    @Test fun captureRejectsChangingAndFailedObservationsButKeepsKnownAbsence() {
        val capture = SourceMetadataCapture(10_000)
        capture.record(book, metadata)
        capture.record(sidecar, null)
        val stable = checkNotNull(capture.fingerprint())
        assertTrue(stable.documents.containsKey(sidecar))
        assertNull(stable.documents[sidecar])
        capture.record(book, metadata.copy(size = 3))
        assertNull(capture.fingerprint())
        assertEquals(2L, stable.documents[book]?.size)
        val failed = SourceMetadataCapture(10_000)
        failed.record(book, metadata)
        failed.fail()
        assertNull(failed.fingerprint())
    }

    @Test fun missingInvalidRecentAndFutureMetadataNeverBecomeReusableLater() {
        for (candidate in listOf(
            metadata.copy(size = null),
            metadata.copy(size = -1),
            metadata.copy(modified = null),
            metadata.copy(modified = 0),
            metadata.copy(modified = 9_999),
            metadata.copy(modified = 20_000),
        )) {
            val capture = SourceMetadataCapture(10_000)
            capture.record(book, candidate)
            assertNull(capture.fingerprint())
        }
        val settled = SourceMetadataCapture(10_000)
        settled.record(book, metadata.copy(modified = 5_000))
        assertNotNull(settled.fingerprint())
    }

    @Test fun nonCurrentSidecarAndFileReplacementInvalidateMetadataOnlyCheck() = runTest {
        val reader = Documents()
        val fingerprint = SourceMetadataFingerprint(reader.values.toMap())
        assertTrue(fingerprint.matches(reader, 10_000))
        reader.values[sidecar] = metadata.copy(size = 8)
        assertFalse(fingerprint.matches(reader, 10_000))
        reader.values[sidecar] = null
        assertTrue(fingerprint.matches(reader, 10_000))
        reader.values[book] = metadata.copy(modified = 2)
        assertFalse(fingerprint.matches(reader, 10_000))
    }

    @Test fun bookRemovalReappearanceAndAbsentSidecarAppearanceInvalidate() = runTest {
        val reader = Documents()
        val present = SourceMetadataFingerprint(reader.values.toMap())
        reader.values[book] = null
        assertFalse(present.matches(reader, 10_000))
        val missing = SourceMetadataFingerprint(reader.values.toMap())
        reader.values[book] = metadata
        assertFalse(missing.matches(reader, 10_000))
        reader.values[sidecar] = metadata.copy(uri = "content://fixture/sidecar")
        assertFalse(present.matches(reader, 10_000))
    }

    @Test fun permissionAndProviderFailuresForceReadWhileCancellationPropagates() = runTest {
        val reader = Documents()
        val fingerprint = SourceMetadataFingerprint(reader.values.toMap())
        reader.failure = SecurityException("Revoked")
        assertFalse(fingerprint.matches(reader, 10_000))
        reader.failure = IOException("Provider unavailable")
        assertFalse(fingerprint.matches(reader, 10_000))
        reader.failure = CancellationException("Cancelled")
        try {
            fingerprint.matches(reader, 10_000)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    private inner class Documents : SourceDocumentReader {
        val values = linkedMapOf(book to metadata, sidecar to null)
        var failure: Exception? = null
        override suspend fun describe(tree: String, path: List<String>): SourceDocument {
            failure?.let { throw it }
            return values[Document(tree, path)] ?: throw MissingSourceFile()
        }

        override suspend fun <T> read(
            tree: String,
            path: List<String>,
            parse: suspend (InputStream) -> T,
        ): T = error("Fingerprint checks must not open documents")
    }
}
