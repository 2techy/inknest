package dev.twotechy.inknest.data.local.database.books

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BookFileKeyTest {
    @Test fun unknownRevisionStaysInItsOwnPartition() {
        val path = "primary:Books/Book.epub"
        val reading = bookFileId("source", path, null, "reading:snapshot")

        assertEquals(reading, bookFileId("source", path, null, "reading:snapshot"))
        assertNotEquals(reading, bookFileId("source", path, null, "reading:nextSnapshot"))
        assertNotEquals(reading, bookFileId("source", path, null, "library:snapshot"))
        assertNotEquals(reading, bookFileId("source", path, "reading:snapshot", "reading:snapshot"))
    }
}
