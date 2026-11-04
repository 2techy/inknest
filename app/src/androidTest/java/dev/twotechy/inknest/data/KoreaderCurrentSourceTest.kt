package dev.twotechy.inknest.data

import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.KoreaderCurrentSource
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.ReadingRefreshException
import dev.twotechy.inknest.data.source.SourceDocument
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.model.BookAvailability
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.InputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class KoreaderCurrentSourceTest {
    private val config = ReadingSourceConfig(
        sourceId = "test",
        koreaderTreeUri = "content://com.android.externalstorage.documents/tree/primary%3Akoreader",
        booksTreeUri = "content://com.android.externalstorage.documents/tree/primary%3ADownload",
    )

    @Test fun lastfileTakesPriorityAndMissingBookRetainsItsIdentity() = runBlocking {
        val files = Files()
        val source = source(files)
        files.history = "return false"
        for (name in listOf("A", "B", "A")) {
            files.settings = "return {lastfile='/sdcard/Download/$name.epub'}"
            val item = source.read(config).current
            assertEquals("primary:Download/$name.epub", item?.id?.path)
            assertEquals(name, item?.title)
        }
        files.bookMissing = true
        files.history = "return {{file='/sdcard/Download/B.epub', time=1}}"
        val missing = source.read(config)
        assertEquals("primary:Download/A.epub", missing.current?.id?.path)
        assertEquals(BookAvailability.Missing, missing.current?.availability)
        assertEquals(ReadingSyncIssue.MissingData, missing.warning)
    }

    @Test fun historyFallbackDistinguishesSuccessfulEmptyFromCorruptSource() = runBlocking {
        val files = Files()
        val source = source(files)
        files.history = "return {{file='/sdcard/Download/B.epub', time=1}}"
        for (settings in listOf(null, "return false", "return {lastfile='../invalid.epub'}")) {
            files.settings = settings
            assertEquals("primary:Download/B.epub", source.read(config).current?.id?.path)
        }
        files.settings = "return {}"
        files.history = "return {}"
        assertNull(source.read(config).current)
        files.settings = "return false"
        try {
            source.read(config)
            fail("Corrupt current settings must not publish an empty reading snapshot")
        } catch (error: ReadingRefreshException) {
            assertEquals(ReadingSyncIssue.InvalidData, error.issue)
        }
    }

    private fun source(files: Files): KoreaderCurrentSource {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return KoreaderCurrentSource(files, BookProbeDataSource(files, context.cacheDir))
    }

    private class Files : SourceDocumentReader {
        var settings: String? = null
        var history = "return {}"
        var bookMissing = false
        override suspend fun describe(tree: String, path: List<String>): SourceDocument {
            if (bookMissing) throw MissingSourceFile()
            return SourceDocument("content://test/book", "application/epub+zip", 1L, 1L)
        }
        override suspend fun <T> read(
            tree: String,
            path: List<String>,
            parse: suspend (InputStream) -> T,
        ): T {
            val text = if (path.single() ==
                "settings.reader.lua"
            ) {
                settings ?: throw MissingSourceFile()
            } else {
                history
            }
            return text.byteInputStream().use { parse(it) }
        }
    }
}
