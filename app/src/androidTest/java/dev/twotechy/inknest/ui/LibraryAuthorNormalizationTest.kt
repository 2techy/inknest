package dev.twotechy.inknest.ui

import dev.twotechy.inknest.ui.library.libraryAuthorKey
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryAuthorNormalizationTest {
    @Test fun unicodeWhitespaceNormalizesOnAndroidRegexEngine() {
        assertEquals(libraryAuthorKey("A B"), libraryAuthorKey("  A\u00a0B  "))
        assertEquals("unknown:", libraryAuthorKey(" "))
    }
}
