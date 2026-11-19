package dev.twotechy.inknest.data

import android.Manifest
import android.content.ContentResolver
import android.content.pm.ProviderInfo
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.source.books.impl.SafBookDataSource
import dev.twotechy.inknest.data.source.koreader.impl.KoreaderCurrentReadingDataSource
import dev.twotechy.inknest.data.source.saf.MissingSourceFileException
import dev.twotechy.inknest.data.source.saf.SafPermissionSnapshot
import dev.twotechy.inknest.data.source.saf.SafSourceAccess
import dev.twotechy.inknest.data.source.saf.SourceDocument
import dev.twotechy.inknest.data.source.saf.SourceMetadataCapture
import dev.twotechy.inknest.data.source.saf.SourceMetadataFingerprint
import dev.twotechy.inknest.data.source.saf.impl.SafTreeReader
import dev.twotechy.inknest.data.source.statistics.impl.StatisticsSnapshotFiles
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.SourceAccessState
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SafTreeReaderTest {
    @Test fun metadataCaptureRejectsReadFailuresAfterSuccessfulMetadataLookup() = runBlocking {
        for (direct in listOf(false, true)) {
            val h = Harness()
            h.provider.add("primary:Books/Book.epub")
            val capture = SourceMetadataCapture(10_000)
            var parsed = false
            withContext(capture) {
                val document = h.reader.describe(h.tree, listOf("Book.epub"))
                checkNotNull(capture.fingerprint())
                val parse: suspend (java.io.InputStream) -> Unit = {
                    parsed = true
                    throw IOException("Injected read failure")
                }
                try {
                    if (direct) {
                        h.reader.readDocument(document, parse)
                    } else {
                        h.reader.read(h.tree, listOf("Book.epub"), parse)
                    }
                    error("Read must fail")
                } catch (_: IOException) { }
            }
            assertTrue(parsed)
            assertEquals(null, capture.fingerprint())
        }
    }

    @Test fun readingFingerprintRequiresUnusedBooksTreeGrantAndProviderAccess() = runBlocking {
        val h = Harness()
        val other = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "primary:Reader").toString()
        for (id in listOf("primary:Books", "primary:Reader")) {
            h.provider.entries[id] = SafTestDocumentsProvider.Entry(
                id,
                null,
                id,
                DocumentsContract.Document.MIME_TYPE_DIR,
            )
        }
        h.provider.add("primary:Books/Book.epub")
        val fingerprint = SourceMetadataFingerprint(
            mapOf(
                SourceMetadataFingerprint.Document(h.tree, listOf("Book.epub")) to
                    h.reader.describe(h.tree, listOf("Book.epub")),
            ),
        )
        val source = KoreaderCurrentReadingDataSource(
            h.reader,
            SafBookDataSource(
                h.reader,
                InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            ),
            access = SafSourceAccess(ContentResolver.wrap(h.provider)),
            now = { 10_000 },
        )
        val config = ReadingSourceConfig("source", h.tree, setOf(other))
        withContext(SafPermissionSnapshot(setOf(h.tree, other))) {
            assertTrue(source.unchanged(config, fingerprint))
        }
        withContext(SafPermissionSnapshot(setOf(h.tree))) {
            assertFalse(source.unchanged(config, fingerprint))
        }
        h.provider.granted = false
        withContext(SafPermissionSnapshot(setOf(h.tree, other))) {
            assertFalse(source.unchanged(config, fingerprint))
        }
    }

    @Test fun metadataCaptureIncludesReadsAndAbsenceButRejectsRevokedAccess() = runBlocking {
        val h = Harness()
        h.provider.add("primary:Books/Book.epub")
        val capture = SourceMetadataCapture(10_000)
        withContext(capture) {
            assertEquals(
                "book",
                h.reader.read(h.tree, listOf("Book.epub")) {
                    it.bufferedReader().readText()
                },
            )
            try {
                h.reader.describe(h.tree, listOf("missing.sdr", "metadata.epub.lua"))
                error("Missing sidecar must fail")
            } catch (_: MissingSourceFileException) { }
        }
        val fingerprint = checkNotNull(capture.fingerprint())
        assertEquals(2, fingerprint.documents.size)
        assertTrue(fingerprint.documents.values.any { it == null })
        val revoked = SourceMetadataCapture(10_000)
        h.readable = false
        withContext(revoked) {
            try {
                h.reader.describe(h.tree, listOf("Book.epub"))
                error("Revoked access must fail")
            } catch (_: SecurityException) { }
        }
        assertEquals(null, revoked.fingerprint())
    }

    @Test fun existingDeepPathsUseOneMetadataQueryAndNoListings() = runBlocking {
        val h = Harness()
        val path = listOf("a", "b", "c", "Café #100% & 你好.epub")
        val id = "primary:Books/" + path.joinToString("/")
        h.provider.add(id)
        val result = h.reader.describe(h.tree, path)
        assertEquals(id, DocumentsContract.getDocumentId(result.uri.toUri()))
        assertEquals(listOf(id), h.provider.metadataQueries)
        assertTrue(h.provider.childQueries.isEmpty())
    }

    @Test fun emptyPathsAndPrimaryRootDoNotInsertExtraSeparators() = runBlocking {
        for (root in listOf("primary:", "primary:Books")) {
            val h = Harness(root)
            h.provider.add(root)
            assertEquals(
                root,
                DocumentsContract.getDocumentId(h.reader.describe(h.tree, emptyList()).uri.toUri()),
            )
            val child = root + if (root == "primary:") "Book.epub" else "/Book.epub"
            h.provider.add(child)
            h.reader.describe(h.tree, listOf("Book.epub"))
            assertEquals(listOf(root, child), h.provider.metadataQueries)
            assertTrue(h.provider.childQueries.isEmpty())
        }
    }

    @Test fun missingDirectIdFallsBackOnceUsingExactDisplayNames() = runBlocking {
        val h = Harness()
        h.provider.add("primary:Books/provider-folder", "primary:Books", "Folder")
        h.provider.add(
            "primary:Books/provider-folder/provider-book",
            "primary:Books/provider-folder",
            "Café.epub",
        )
        val result = h.reader.describe(h.tree, listOf("Folder", "Café.epub"))
        assertEquals(
            "primary:Books/provider-folder/provider-book",
            DocumentsContract.getDocumentId(result.uri.toUri()),
        )
        assertEquals(
            listOf("primary:Books", "primary:Books/provider-folder"),
            h.provider.childQueries,
        )
        assertEquals(2, h.provider.metadataQueries.size)
    }

    @Test fun missingFilesAndCaseMismatchesDoNotGuessNames() {
        val h = Harness()
        h.provider.add("primary:Books/Book.epub", "primary:Books", "Book.epub")
        for (name in listOf("missing.epub", "book.epub")) {
            assertThrows(MissingSourceFileException::class.java) {
                runBlocking { h.reader.describe(h.tree, listOf(name)) }
            }
        }
        assertEquals(2, h.provider.metadataQueries.size)
        assertEquals(listOf("primary:Books", "primary:Books"), h.provider.childQueries)
    }

    @Test fun nullForMissingDirectIdFallsBackAndRetainsMissingFileClassification() = runBlocking {
        val h = Harness()
        h.provider.missingMetadata = SafTestDocumentsProvider.MissingMetadata.Null
        h.provider.add("primary:Books/provider-book", "primary:Books", "Book.epub")
        val found = h.reader.describe(h.tree, listOf("Book.epub"))
        assertEquals(
            "primary:Books/provider-book",
            DocumentsContract.getDocumentId(found.uri.toUri()),
        )
        assertThrows(MissingSourceFileException::class.java) {
            runBlocking { h.reader.describe(h.tree, listOf("missing.epub")) }
        }
        assertEquals(listOf("primary:Books", "primary:Books"), h.provider.childQueries)
    }

    @Test fun missingWalRejectedByProviderDoesNotPreventStatisticsCopy() = runBlocking {
        val h = Harness()
        h.provider.missingMetadata = SafTestDocumentsProvider.MissingMetadata.Rejected
        h.provider.add("primary:Books/settings", "primary:Books", "settings")
        h.provider.add(
            "primary:Books/settings/statistics.sqlite3",
            "primary:Books/settings",
            "statistics.sqlite3",
        )
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val directory = Files.createTempDirectory(cache.toPath(), "stats-missing-wal-").toFile()
        try {
            withContext(h.reader.permissionSnapshot()) {
                assertFalse(StatisticsSnapshotFiles(h.reader, h.tree).copyTo(directory).hasWal)
            }
            assertEquals(listOf("statistics.sqlite3"), directory.list().orEmpty().toList())
            assertEquals("book", File(directory, "statistics.sqlite3").readText())
            assertEquals(1, h.permissionReads)
        } finally {
            check(directory.canonicalFile.parentFile == cache.canonicalFile)
            directory.deleteRecursively()
        }
    }

    @Test fun invalidPathsAndVolumesFailBeforeQuerying() {
        val h = Harness()
        for (path in listOf(listOf(".."), listOf("a/b"), listOf("\u0000"))) {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { h.reader.describe(h.tree, path) }
            }
        }
        val unsupported = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "other:Books")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { h.reader.describe(unsupported.toString(), emptyList()) }
        }
        assertTrue(h.provider.metadataQueries.isEmpty())
        assertTrue(h.provider.childQueries.isEmpty())
    }

    @Test fun fallbackAndDirectReadsRejectSiblingPrefixEscapes() {
        val h = Harness()
        h.provider.add("primary:BooksElsewhere/book", "primary:Books", "Book.epub")
        assertThrows(IOException::class.java) {
            runBlocking { h.reader.describe(h.tree, listOf("Book.epub")) }
        }
        val escaped = DocumentsContract.buildDocumentUriUsingTree(
            h.tree.toUri(),
            "primary:BooksElsewhere/book",
        )
        assertThrows(IOException::class.java) {
            runBlocking {
                h.reader.readDocument(SourceDocument(escaped.toString(), null, null, null)) {
                    error("Must not parse")
                }
            }
        }
    }

    @Test fun permissionSnapshotsReadGrantsOnceAndDoNotOutliveTheirOperation() = runBlocking {
        val h = Harness()
        h.provider.add("primary:Books/Book.epub")
        withContext(h.reader.permissionSnapshot()) {
            repeat(5) { h.reader.describe(h.tree, listOf("Book.epub")) }
        }
        assertEquals(1, h.permissionReads)
        h.readable = false
        assertThrows(SecurityException::class.java) {
            runBlocking { h.reader.describe(h.tree, listOf("Book.epub")) }
        }
        assertEquals(2, h.permissionReads)
        assertEquals(5, h.provider.metadataQueries.size)
    }

    @Test fun sourceInspectionUsesTheSamePermissionSnapshot() = runBlocking {
        val h = Harness()
        h.provider.entries["primary:Books"] = SafTestDocumentsProvider.Entry(
            "primary:Books",
            null,
            "Books",
            DocumentsContract.Document.MIME_TYPE_DIR,
        )
        val access = SafSourceAccess(ContentResolver.wrap(h.provider))
        withContext(h.reader.permissionSnapshot()) {
            assertEquals(SourceAccessState.Granted, access.inspect(h.tree))
        }
        assertEquals(1, h.permissionReads)
    }

    @Test fun revokedProviderPermissionStillFailsInsideSnapshot() = runBlocking {
        val h = Harness()
        h.provider.add("primary:Books/Book.epub")
        withContext(h.reader.permissionSnapshot()) {
            h.reader.describe(h.tree, listOf("Book.epub"))
            h.provider.granted = false
            try {
                h.reader.describe(h.tree, listOf("Book.epub"))
                error("Revoked provider access must fail")
            } catch (_: SecurityException) { }
        }
        assertEquals(1, h.permissionReads)
        assertTrue(h.provider.childQueries.isEmpty())
    }

    @Test fun directReadsCheckCurrentGrantBeforeOpening() = runBlocking {
        val h = Harness()
        h.provider.add("primary:Books/Book.epub")
        val document = h.reader.describe(h.tree, listOf("Book.epub"))
        assertEquals("book", h.reader.readDocument(document) { it.bufferedReader().readText() })
        assertEquals(2, h.permissionReads)
        h.readable = false
        assertThrows(SecurityException::class.java) {
            runBlocking { h.reader.readDocument(document) { error("Must not parse") } }
        }
        assertEquals(3, h.permissionReads)
    }

    @Test fun providerErrorsRemainDistinctFromMissingFiles() {
        val h = Harness()
        h.provider.add("primary:Books/Book.epub", "primary:Books", "Book.epub")
        h.provider.queryFailure = IllegalArgumentException("provider failure")
        val error = assertThrows(IOException::class.java) {
            runBlocking { h.reader.describe(h.tree, listOf("Book.epub")) }
        }
        assertTrue(error.cause is IllegalArgumentException)
        assertEquals(listOf("primary:Books"), h.provider.childQueries)
        h.provider.queryFailure = null
        h.provider.add("primary:Books/Book.epub", "primary:Books", "Book.epub")
        h.provider.nullMetadata = true
        val metadataError = assertThrows(IOException::class.java) {
            runBlocking { h.reader.describe(h.tree, listOf("Book.epub")) }
        }
        assertTrue(metadataError !is MissingSourceFileException)
        assertEquals(listOf("primary:Books", "primary:Books"), h.provider.childQueries)
    }

    private class Harness(root: String = "primary:Books") {
        val tree = DocumentsContract.buildTreeDocumentUri(AUTHORITY, root).toString()
        val provider = SafTestDocumentsProvider().apply {
            attachInfo(
                InstrumentationRegistry.getInstrumentation().targetContext,
                ProviderInfo().apply {
                    authority = AUTHORITY
                    exported = true
                    grantUriPermissions = true
                    readPermission = Manifest.permission.MANAGE_DOCUMENTS
                    writePermission = Manifest.permission.MANAGE_DOCUMENTS
                },
            )
        }
        var readable = true
        var permissionReads = 0
        val reader = SafTreeReader(ContentResolver.wrap(provider), readableTrees = {
            permissionReads++
            if (readable) setOf(tree) else emptySet()
        })
    }

    private companion object {
        const val AUTHORITY = "com.android.externalstorage.documents"
    }
}
