package dev.twotechy.inknest.data

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import dev.twotechy.inknest.data.local.DataStoreAppUpdateStore
import dev.twotechy.inknest.data.source.AppReleaseJson
import dev.twotechy.inknest.data.source.GitHubReleaseSource
import dev.twotechy.inknest.data.source.UpdateHttpTransport
import dev.twotechy.inknest.model.AppRelease
import dev.twotechy.inknest.model.UpdateSnapshot
import dev.twotechy.inknest.platform.AndroidUpdateApkVerifier
import dev.twotechy.inknest.platform.AndroidUpdateInstallPlatform
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun releaseMetadataRoundTripsAndRejectsUnsafeOrAmbiguousAssets() {
        assertEquals(RELEASE, AppReleaseJson.decode(AppReleaseJson.encode(RELEASE)))
        val invalid = listOf(
            JSONObject(AppReleaseJson.encode(RELEASE)).put("prerelease", true),
            JSONObject(AppReleaseJson.encode(RELEASE)).put("draft", true),
            JSONObject(AppReleaseJson.encode(RELEASE)).put("tag_name", "v2026.11.0-beta"),
            JSONObject(AppReleaseJson.encode(RELEASE)).apply {
                getJSONArray("assets").getJSONObject(0)
                    .put("browser_download_url", "https://evil.example/update.apk")
            },
            JSONObject(AppReleaseJson.encode(RELEASE)).apply {
                getJSONArray("assets").put(getJSONArray("assets").getJSONObject(0))
            },
            JSONObject(AppReleaseJson.encode(RELEASE)).apply {
                getJSONArray("assets").getJSONObject(0).put("size", Long.MAX_VALUE)
            },
        )
        invalid.forEach { metadata -> expectIo { AppReleaseJson.decode(metadata.toString()) } }
    }

    @Test fun checksumMustMatchExactAssetAndAppearOnlyOnce() = runBlocking {
        val transport = TextTransport()
        val source = GitHubReleaseSource(transport)
        transport.body = "$HASH  ${RELEASE.apkName}\n${"b".repeat(64)}  other.apk\n"
        assertEquals(HASH, source.checksum(RELEASE))
        transport.body = "$HASH  wrong.apk\n"
        expectSuspendIo { source.checksum(RELEASE) }
        transport.body = "$HASH  ${RELEASE.apkName}\n$HASH  ${RELEASE.apkName}\n"
        expectSuspendIo { source.checksum(RELEASE) }
    }

    @Test fun updatePreferencesPreserveUnrelatedDataAndSurviveNewStoreWrapper() = runBlocking {
        val file = File(context.cacheDir, "update-test-${UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            val pins = stringPreferencesKey("pinned_component_ids")
            dataStore.edit { it[pins] = "reader/Activity" }
            val store = DataStoreAppUpdateStore(dataStore)
            val saved = UpdateSnapshot(
                automatic = false,
                attemptedAt = 123,
                checkedAt = 124,
                release = RELEASE,
                downloadedSha256 = HASH,
            )
            store.update { saved }
            assertEquals(saved, DataStoreAppUpdateStore(dataStore).snapshots.first())
            assertEquals("reader/Activity", dataStore.data.first()[pins])
        } finally {
            scope.cancel()
            file.delete()
        }
    }

    @Test fun providerIsPrivateAndRefusesPathsOutsideUpdateDirectory() {
        val authority = "${context.packageName}.updates"
        val provider = context.packageManager.resolveContentProvider(authority, 0)!!
        assertFalse(provider.exported)
        assertTrue(provider.grantUriPermissions)
        val uri = FileProvider.getUriForFile(
            context,
            authority,
            File(context.filesDir, "updates/update.apk"),
        )
        assertEquals("content", uri.scheme)
        assertEquals(authority, uri.authority)
        var rejected = false
        try {
            FileProvider.getUriForFile(context, authority, File(context.filesDir, "private.txt"))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test fun installerBuildsPermissionAndContentUriIntentsWithoutLaunchingActivities() {
        val captured = CapturingContext(context)
        val platform = AndroidUpdateInstallPlatform(captured)
        platform.requestPermission()
        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, captured.intent!!.action)
        assertEquals("package:${context.packageName}", captured.intent!!.dataString)
        platform.install("update.apk")
        val intent = captured.intent!!
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("content", intent.data!!.scheme)
        assertEquals("application/vnd.android.package-archive", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test fun verifierRejectsInstalledVersionAndDifferentPackage() = runBlocking {
        val verifier = AndroidUpdateApkVerifier(context)

        @Suppress("DEPRECATION")
        val current = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentRelease = RELEASE.copy(version = current.versionName!!)
        expectSuspendIo {
            verifier.verify(File(context.applicationInfo.sourceDir), currentRelease)
        }
        val testContext = InstrumentationRegistry.getInstrumentation().context
        expectSuspendIo {
            verifier.verify(File(testContext.applicationInfo.sourceDir), RELEASE)
        }
    }

    private fun expectIo(block: () -> Unit) {
        var rejected = false
        try {
            block()
        } catch (_: IOException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    private suspend fun expectSuspendIo(block: suspend () -> Unit) {
        var rejected = false
        try {
            block()
        } catch (_: IOException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    private class CapturingContext(base: Context) : ContextWrapper(base) {
        var intent: Intent? = null
        override fun startActivity(intent: Intent) {
            this.intent = intent
        }
    }

    private class TextTransport : UpdateHttpTransport {
        var body = ""
        override suspend fun text(url: String, limit: Int): String = body
        override suspend fun download(
            url: String,
            target: File,
            size: Long,
            progress: (Int) -> Unit,
        ) = Unit
    }

    private companion object {
        val HASH = "a".repeat(64)
        val RELEASE = AppRelease(
            "2026.11.0",
            "Release notes",
            "2026-11-10T00:00:00Z",
            "https://github.com/2techy/inknest/releases/download/v2026.11.0/InkNest-2026.11.0.apk",
            "https://github.com/2techy/inknest/releases/download/v2026.11.0/SHA256SUMS.txt",
            4,
        )
    }
}
