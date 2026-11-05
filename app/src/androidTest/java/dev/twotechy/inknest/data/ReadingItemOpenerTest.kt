package dev.twotechy.inknest.data

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceDocument
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.platform.BookIntentLauncher
import dev.twotechy.inknest.platform.OpenResult
import dev.twotechy.inknest.platform.ReadingItemOpener
import dev.twotechy.inknest.platform.koreaderDocumentIntent
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingItemOpenerTest {
    @Test fun originalDocumentsDispatchWithoutConfirmationForEverySelection() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.configure()
            val intents = mutableListOf<Intent>()
            val opener = fixture.recordingOpener(intents)
            for (name in listOf("A book.epub", "B book.epub", "A book.epub")) {
                assertEquals(OpenResult.Dispatched, opener.open(fixture.id(name)))
                val intent = intents.last()
                assertEquals(Intent.ACTION_VIEW, intent.action)
                assertEquals("org.koreader.launcher", intent.`package`)
                assertEquals("application/epub+zip", intent.type)
                assertEquals(
                    "primary:Download/$name",
                    DocumentsContract.getDocumentId(intent.data!!),
                )
                assertEquals(intent.data, intent.clipData!!.getItemAt(0).uri)
                assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
                assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
            }
            assertEquals(3, intents.size)
            assertEquals(0, fixture.documents.reads)
        } finally {
            fixture.close()
        }
    }

    @Test fun sourceFilePermissionAndReaderErrorsNeverFallBackToLaunchingTheApp() = runBlocking {
        val fixture = Fixture()
        try {
            fixture.configure()
            val intents = mutableListOf<Intent>()
            val opener = fixture.recordingOpener(intents)
            val id = fixture.id("A.epub")
            assertEquals(OpenResult.SourceChanged, opener.open(id.copy(sourceId = "old")))
            fixture.documents.missing = true
            assertEquals(OpenResult.MissingFile, opener.open(id))
            fixture.documents.missing = false
            fixture.documents.denied = true
            assertEquals(OpenResult.PermissionLost, opener.open(id))
            fixture.documents.denied = false
            fixture.config.setSourcePackage("dev.twotechy.missing.reader")
            assertEquals(OpenResult.MissingApp, fixture.realOpener.open(id))
            fixture.config.setSourcePackage(fixture.context.packageName)
            assertEquals(OpenResult.Unsupported, fixture.realOpener.open(id))
            fixture.documents.resolving = { fixture.config.setSourcePackage("changed.reader") }
            assertEquals(OpenResult.SourceChanged, opener.open(id))
            assertTrue(intents.isEmpty())
            assertEquals(0, fixture.documents.reads)
        } finally {
            fixture.close()
        }
    }

    @Test fun readerOwnedUriPreservesTheOriginalPathWithoutForeignGrants() {
        val path = "webdav/folder/Book with spaces \u0111\u1eb9p.epub"
        val document = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(TREE), "primary:$path")
        val original = Intent(Intent.ACTION_VIEW).apply {
            setPackage("org.koreader.launcher")
            setDataAndType(document, "application/epub+zip")
            clipData = ClipData.newRawUri("Book", document)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val prepared = koreaderDocumentIntent(original) { "org.koreader.launcher" }
        assertEquals("org.koreader.launcher.provider", prepared.data!!.authority)
        assertEquals("/external/$path", prepared.data!!.path)
        assertEquals(prepared.data, prepared.clipData!!.getItemAt(0).uri)
        assertEquals("application/epub+zip", prepared.type)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, prepared.flags)
        assertEquals(document, original.data)
        assertTrue(koreaderDocumentIntent(original) { null } === original)
        assertTrue(koreaderDocumentIntent(original) { "wrong.owner" } === original)
        val unsafe = Intent(original).setDataAndType(
            DocumentsContract.buildDocumentUriUsingTree(
                Uri.parse(TREE),
                "primary:webdav/../secret.epub",
            ),
            original.type,
        )
        assertTrue(koreaderDocumentIntent(unsafe) { "org.koreader.launcher" } === unsafe)
        val secondary = Intent(original).setDataAndType(
            DocumentsContract.buildDocumentUriUsingTree(Uri.parse(TREE), "volume:Book.epub"),
            original.type,
        )
        assertTrue(koreaderDocumentIntent(secondary) { "org.koreader.launcher" } === secondary)
    }

    private class Fixture {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val directory = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "opener-",
        ).toFile()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val store = PreferenceDataStoreFactory.create(scope = scope) {
            File(directory, "settings.preferences_pb")
        }
        val config = ReadingSourceConfigRepository(store)
        val documents = Documents()
        private val books = BookProbeDataSource(documents, directory)
        val realOpener = ReadingItemOpener(context, config, books)
        suspend fun configure() {
            val before = config.config.first()
            config.activateTree(before, config.treeCandidate(before, SourceFolder.KOReader, TREE))
            config.setSourcePackage("org.koreader.launcher")
        }
        suspend fun id(name: String) =
            ReadingItemId(checkNotNull(config.config.first().sourceId), "primary:Download/$name")
        fun recordingOpener(intents: MutableList<Intent>) = ReadingItemOpener(
            context,
            config,
            books,
            BookIntentLauncher {
                intents.add(it)
                OpenResult.Dispatched
            },
        )
        fun close() {
            scope.cancel()
            directory.deleteRecursively()
        }
    }

    private class Documents : SourceDocumentReader {
        var missing = false
        var denied = false
        var reads = 0
        var resolving: suspend () -> Unit = {}
        override suspend fun describe(tree: String, path: List<String>): SourceDocument {
            resolving()
            if (denied) throw SecurityException()
            if (missing) throw MissingSourceFile()
            val id = "primary:Download/" + path.joinToString("/")
            val uri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(tree), id)
            return SourceDocument(uri.toString(), "application/epub+zip", 1L, 1L)
        }
        override suspend fun <T> read(
            tree: String,
            path: List<String>,
            parse: suspend (InputStream) -> T,
        ): T {
            reads++
            error("Opening must not copy or extract the book")
        }
    }
    private companion object {
        const val TREE = "content://com.android.externalstorage.documents/tree/primary%3ADownload"
    }
}
