package dev.twotechy.inknest.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrimaryTreePathTest {
    @Test fun `relative identity preserves unicode and rejects sibling roots`() {
        val root = PrimaryTreePath.parse("primary:Books")!!
        assertEquals(
            listOf("Tiếng Việt", "It's a book.epub"),
            PrimaryTreePath.parse("primary:Books/Tiếng Việt/It's a book.epub")!!.relativeTo(root),
        )
        assertNull(PrimaryTreePath.parse("primary:Books2/a.epub")!!.relativeTo(root))
        assertNull(PrimaryTreePath.parse("primary:books/a.epub")!!.relativeTo(root))
    }

    @Test fun `invalid segments and other volumes are rejected`() {
        listOf("other:Books", "primary:Books/../a", "primary:./Books", "primary:Books//a").forEach {
            assertNull(PrimaryTreePath.parse(it))
        }
    }

    @org.junit.Test fun overlapsUseSegmentsAndRetainCase() {
        val books = checkNotNull(PrimaryTreePath.parse("primary:Books"))
        org.junit.Assert.assertTrue(books.overlaps(books))
        org.junit.Assert.assertTrue(
            books.overlaps(checkNotNull(PrimaryTreePath.parse("primary:Books/Fiction"))),
        )
        org.junit.Assert.assertTrue(books.overlaps(checkNotNull(PrimaryTreePath.parse("primary:"))))
        org.junit.Assert.assertFalse(
            books.overlaps(checkNotNull(PrimaryTreePath.parse("primary:Bookshelf"))),
        )
        org.junit.Assert.assertFalse(
            books.overlaps(checkNotNull(PrimaryTreePath.parse("primary:books"))),
        )
    }
}
