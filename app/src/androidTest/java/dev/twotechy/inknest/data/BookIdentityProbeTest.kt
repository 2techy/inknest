package dev.twotechy.inknest.data

import android.os.Environment
import android.provider.DocumentsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceDocument
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.io.InputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookIdentityProbeTest {
    private class Files : SourceDocumentReader {
        var missing = false
        override suspend fun describe(tree: String, path: List<String>): SourceDocument {
            if (missing) throw MissingSourceFile()
            return SourceDocument(
                "content://test/" + path.joinToString("/"),
                "application/epub+zip",
                1,
                1,
            )
        }
        override suspend fun <T> read(
            tree: String,
            path: List<String>,
            parse: suspend (InputStream) -> T,
        ): T {
            check(path.drop(1) == listOf("Title.sdr", "metadata.epub.lua"))
            val percent = if (path.first() == "A") 0.2 else 0.7
            val script = "return {percent_finished=$percent,summary={status='reading'}}"
            return script.byteInputStream().use { parse(it) }
        }
    }

    @Test fun sameBasenamesKeepDistinctIdentityAndSidecars() = runBlocking {
        val files = Files()
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val probe = BookProbeDataSource(files, cache)
        val tree = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Books",
        ).toString()
        val config = ReadingSourceConfig(booksTreeUri = tree)
        val root = Environment.getExternalStorageDirectory().canonicalPath
        val a = probe.resolve(config, "$root/Books/A/Title.epub")
        val b = probe.resolve(config, "$root/Books/B/Title.epub")
        assertNotEquals(a.document.uri, b.document.uri)
        assertEquals(0.2, checkNotNull(probe.sidecar(a).percent), 0.00001)
        assertEquals(0.7, checkNotNull(probe.sidecar(b).percent), 0.00001)
    }

    @Test fun deletedOriginalFileIsNotResolvedByItsBasename() = runBlocking {
        val files = Files().apply { missing = true }
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val tree = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Books",
        ).toString()
        val root = Environment.getExternalStorageDirectory().canonicalPath
        try {
            BookProbeDataSource(
                files,
                cache,
            ).resolve(ReadingSourceConfig(booksTreeUri = tree), "$root/Books/A/Title.epub")
            fail("Missing file must not resolve")
        } catch (_: MissingSourceFile) { }
    }
}
