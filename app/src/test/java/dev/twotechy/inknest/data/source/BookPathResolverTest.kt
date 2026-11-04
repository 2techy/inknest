package dev.twotechy.inknest.data.source

import dev.twotechy.inknest.data.source.epub.archivePath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookPathResolverTest {
    private val resolver = BookPathResolver(setOf("/storage/emulated/0", "/sdcard"))

    @Test fun `source paths require verified aliases and segment boundaries`() {
        assertEquals(
            listOf("It's Tiếng Việt.epub"),
            resolver.relative("/sdcard/Books/It's Tiếng Việt.epub", "primary:Books"),
        )
        assertNull(resolver.relative("/sdcard/Books2/a.epub", "primary:Books"))
        assertNull(resolver.relative("/sdcard/books/a.epub", "primary:Books"))
        assertNull(resolver.relative("/storage/emulated/1/Books/a.epub", "primary:Books"))
        assertNull(resolver.relative("/sdcard/Books/../secret", "primary:Books"))
    }

    @Test fun `archive links stay inside the archive and decode escaped names`() {
        assertEquals("cover art.jpg", archivePath("OPS/book.opf", "../cover%20art.jpg"))
        assertNull(archivePath("OPS/book.opf", "../../private"))
        assertNull(archivePath("OPS/book.opf", "%2e%2e/%2e%2e/private"))
        assertNull(archivePath("OPS/book.opf", "https://example.test/a"))
        assertNull(archivePath("OPS/book.opf", "//host/a"))
        assertNull(archivePath("OPS/book.opf", "/a"))
    }
}
