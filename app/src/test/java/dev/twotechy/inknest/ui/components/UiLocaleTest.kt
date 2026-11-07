package dev.twotechy.inknest.ui.components

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class UiLocaleTest {
    @Test fun `formatting follows supported system order and preserves regions`() {
        assertEquals("vi-VN", resolveUiLocale("fr-FR,vi-VN,en-US").toLanguageTag())
        assertEquals("en-GB", resolveUiLocale("fr-FR,en-GB,vi-VN").toLanguageTag())
        assertEquals(Locale.ENGLISH, resolveUiLocale("fr-FR,de-DE"))
        assertEquals(Locale.ENGLISH, resolveUiLocale(""))
    }
}
