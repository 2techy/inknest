package dev.twotechy.inknest.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test fun `locale selection supports regions and empty system locales`() {
        assertEquals(AppLanguage.English, AppLanguage.fromLanguageTag("en-US"))
        assertEquals(AppLanguage.Vietnamese, AppLanguage.fromLanguageTag("vi-VN"))
        assertEquals(AppLanguage.System, AppLanguage.fromLanguageTag(null))
        assertEquals(AppLanguage.System, AppLanguage.fromLanguageTag(""))
        assertEquals(AppLanguage.System, AppLanguage.fromLanguageTag("fr-FR"))
    }
}
