package dev.twotechy.inknest.ui

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageResourcesTest {
    @Test fun supportedResourcesAndUnsupportedFallback() {
        assertEquals("Thư viện", localized("vi-VN", R.string.library_title))
        assertEquals("Library", localized("en-US", R.string.library_title))
        assertEquals("Library", localized("fr-FR", R.string.library_title))
        assertEquals("Thư viện", localized("fr-FR,vi-VN,en-US", R.string.library_title))
        assertEquals("Library", localized("fr-FR,en-GB,vi-VN", R.string.library_title))
        assertEquals("Tiếng Việt", localized("en-US", R.string.language_vietnamese))
        assertEquals("English", localized("vi-VN", R.string.language_english))
    }

    private fun localized(tag: String, resource: Int): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(tag))
        }
        return context.createConfigurationContext(configuration).getString(resource)
    }
}
