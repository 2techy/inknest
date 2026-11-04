package dev.twotechy.inknest.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.SourceDocument
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingOpenMode
import dev.twotechy.inknest.platform.OpenResult
import dev.twotechy.inknest.platform.ReadingItemOpener
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingItemOpenerTest {
    @Test fun unavailableSourceAndDirectFileFailuresNeverDispatchAnIntent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Files.createTempDirectory(
            context.cacheDir.toPath(),
            "opener-test-",
        ).toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) {
            File(directory, "settings.preferences_pb")
        }
        try {
            val config = ReadingSourceConfigRepository(store)
            val before = config.config.first()
            config.activateTree(
                before,
                config.treeCandidate(
                    before,
                    SourceFolder.KOReader,
                    "content://com.android.externalstorage.documents/tree/primary%3ADownload",
                ),
            )
            val documents = Documents()
            val opener =
                ReadingItemOpener(context, config, BookProbeDataSource(documents, directory))
            val id = ReadingItemId(
                checkNotNull(config.config.first().sourceId),
                "primary:Download/A.epub",
            )
            assertEquals(OpenResult.SourceChanged, opener.open(id.copy(sourceId = "old")))
            config.setSourcePackage("dev.twotechy.missing.reader")
            assertEquals(OpenResult.MissingApp, opener.open(id))
            config.setSourcePackage(context.packageName)
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = checkNotNull(info.versionName)
            config.recordOpenMode(ReadingOpenMode.DirectConfirmed, "older-version")
            assertFalse(opener.isDirect(config.config.first()))
            config.recordOpenMode(ReadingOpenMode.DirectConfirmed, version)
            assertTrue(opener.isDirect(config.config.first()))
            assertEquals(OpenResult.MissingFile, opener.open(id))
            documents.denied = true
            assertEquals(OpenResult.PermissionLost, opener.open(id))
        } finally {
            scope.cancel()
            directory.deleteRecursively()
        }
    }

    private class Documents : SourceDocumentReader {
        var denied = false
        override suspend fun describe(tree: String, path: List<String>): SourceDocument {
            if (denied) throw SecurityException()
            throw MissingSourceFile()
        }
        override suspend fun <T> read(
            tree: String,
            path: List<String>,
            parse: suspend (InputStream) -> T,
        ): T = error("Opening must not extract or copy the book")
    }
}
