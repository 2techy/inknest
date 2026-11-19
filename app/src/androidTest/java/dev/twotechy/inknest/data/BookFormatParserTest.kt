package dev.twotechy.inknest.data

import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.source.books.impl.SafBookDataSource
import dev.twotechy.inknest.data.source.formats.BookFormatParser
import dev.twotechy.inknest.data.source.formats.cbz.naturalCompare
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookFormatParserTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun longFb2AnnotationRetainsItsBeginningWithoutArtwork() = runBlocking {
        val file = File.createTempFile("annotation", ".fb2", context.cacheDir)
        try {
            file.writeText(
                "<FictionBook><description><title-info><book-title>Title</book-title>" +
                    "<author><nickname>First</nickname></author>" +
                    "<author><nickname>Second</nickname></author>" +
                    "<annotation><p>Beginning " + "x".repeat(40_000) +
                    "</p></annotation></title-info></description><body/></FictionBook>",
            )
            val result = BookFormatParser().read(file, "fb2") {
                org.junit.Assert.fail("Unexpected cover")
            }
            assertEquals("Title", result.title)
            assertEquals("First", result.author)
            assertTrue(checkNotNull(result.description).startsWith("Beginning "))
            assertTrue(checkNotNull(result.description).toByteArray().size <= 32 * 1024)
            assertFalse(result.cover)
        } finally {
            file.delete()
        }
    }

    @Test fun cancellationRemovesPrivateTemporaryCopy() = runBlocking {
        val directory = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "cancel-metadata-",
        ).toFile()
        val documents = object : dev.twotechy.inknest.data.source.saf.SourceDocumentReader {
            override suspend fun describe(tree: String, path: List<String>) =
                error("Already resolved")
            override suspend fun <T> read(
                tree: String,
                path: List<String>,
                parse: suspend (java.io.InputStream) -> T,
            ): T = throw kotlinx.coroutines.CancellationException("Source changed")
        }
        try {
            val source = dev.twotechy.inknest.data.source.books.impl.SafBookDataSource(
                documents,
                directory,
            )
            val book = dev.twotechy.inknest.data.source.books.ResolvedBook(
                "primary:Books/book.mobi",
                "tree",
                listOf("book.mobi"),
                dev.twotechy.inknest.data.source.saf.SourceDocument(
                    "content://fixture/book",
                    null,
                    null,
                    null,
                ),
            )
            try {
                source.metadata(book)
                org.junit.Assert.fail("Cancellation swallowed")
            } catch (_: kotlinx.coroutines.CancellationException) { }
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun archiveOrderComparesNumbersWithoutIntegerOverflow() {
        assertTrue(naturalCompare("2.jpg", "10.jpg") < 0)
        assertTrue(naturalCompare("99999999999999999999.jpg", "100000000000000000000.jpg") < 0)
        assertFalse(naturalCompare("a.jpg", "a.jpg") != 0)
    }

    @Test fun malformedMetadataDoesNotBecomeSuccessful() = runBlocking {
        val file = File.createTempFile("broken-book", ".mobi", context.cacheDir)
        try {
            file.writeBytes(ByteArray(100))
            try {
                BookFormatParser().read(file, "mobi") {}
                org.junit.Assert.fail("Invalid PalmDB accepted")
            } catch (_: java.io.IOException) { }
            file.writeText(
                "<!DOCTYPE FictionBook [<!ENTITY secret SYSTEM 'file:///secret'>]><FictionBook>&secret;</FictionBook>",
            )
            try {
                BookFormatParser().read(file, "fb2") {}
                org.junit.Assert.fail("DTD accepted")
            } catch (_: java.io.IOException) { }
        } finally {
            file.delete()
        }
    }
}
