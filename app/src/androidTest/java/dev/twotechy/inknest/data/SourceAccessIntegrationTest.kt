package dev.twotechy.inknest.data

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.InkNestApplication
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.model.SourceAccessState
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

// Explicit opt-in only: exercises the test device's selected trees, then revokes its Books
// grant to verify recovery through the actual system picker. Never runs in the default suite.
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SourceAccessIntegrationTest {
    private fun enabled() =
        InstrumentationRegistry.getArguments().getString("source_access_integration") == "true"
    private fun container(): dev.twotechy.inknest.di.AppContainer {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext
        return (application as InkNestApplication).container
    }

    @Test fun aWrongReaderTreeKeepsSourceAndPins() = runBlocking {
        assumeTrue(enabled())
        val container = container()
        val repository = checkNotNull(container.readingSourceConfigRepository)
        val before = repository.config.first()
        assumeTrue(before.koreaderTreeUri != null && before.booksTreeUri != null)
        assumeTrue(before.koreaderTreeUri != before.booksTreeUri)
        val pins = container.userPreferencesRepository.pinnedComponentIds.first()
        val prefs = container.userPreferencesRepository.appPreferences.first()
        try {
            container.sourceSettingsRepository.connectTree(
                SourceFolder.KOReader,
                checkNotNull(before.booksTreeUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            )
            fail("Book library cannot replace the KOReader data directory")
        } catch (_: IOException) { }
        assertEquals(before, repository.config.first())
        assertEquals(pins, container.userPreferencesRepository.pinnedComponentIds.first())
        assertEquals(prefs, container.userPreferencesRepository.appPreferences.first())
    }

    @Test fun bRevokedBooksGrantIsIndependentOfReaderGrant() = runBlocking {
        assumeTrue(enabled())
        val container = container()
        val repository = checkNotNull(container.readingSourceConfigRepository)
        val config = repository.config.first()
        val access = checkNotNull(container.safSourceAccess)
        assumeTrue(config.booksTreeUri != null && config.koreaderTreeUri != null)
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        resolver.releasePersistableUriPermission(
            Uri.parse(config.booksTreeUri),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        assertEquals(SourceAccessState.PermissionLost, access.inspect(config.booksTreeUri))
        assertEquals(SourceAccessState.Granted, access.inspect(config.koreaderTreeUri))
        assertEquals(config, repository.config.first())
    }
}
