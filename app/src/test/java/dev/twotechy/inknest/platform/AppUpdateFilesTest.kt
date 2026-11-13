package dev.twotechy.inknest.platform

import dev.twotechy.inknest.data.source.AppReleaseSource
import dev.twotechy.inknest.model.AppRelease
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateFilesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun completedDownloadIsVerifiedAgainOnRestoreAndBeforeEveryInstall() = runTest {
        val directory = temporary.newFolder()
        val source = FileSource()
        var verifications = 0
        val verifier = UpdateApkVerifier { _, _ -> verifications++ }
        val files =
            PrivateAppUpdateFiles(
                directory,
                source,
                verifier,
                StandardTestDispatcher(testScheduler),
            )
        val sha = files.download(RELEASE, {}, {})
        assertEquals(HASH, sha)
        assertFalse(File(directory, "update.apk.part").exists())
        assertTrue(files.restore(RELEASE, sha))
        assertEquals("update.apk", files.prepare(RELEASE, sha))
        assertEquals(3, verifications)
        File(directory, "update.apk").writeText("tampered")
        assertFalse(files.restore(RELEASE, sha))
        assertFalse(File(directory, "update.apk").exists())
    }

    @Test fun wrongChecksumAndIncompleteFilesNeverBecomeInstallable() = runTest {
        val directory = temporary.newFolder()
        val source = FileSource()
        val files = PrivateAppUpdateFiles(
            directory,
            source,
            UpdateApkVerifier { _, _ -> },
            StandardTestDispatcher(testScheduler),
        )
        source.body = "bad!"
        expectIo { files.download(RELEASE, {}, {}) }
        assertFalse(File(directory, "update.apk").exists())
        source.body = "x"
        expectIo { files.download(RELEASE, {}, {}) }
        assertFalse(File(directory, "update.apk.part").exists())
    }

    @Test fun rejectedPackageOrSignatureNeverBecomesInstallable() = runTest {
        val directory = temporary.newFolder()
        val files = PrivateAppUpdateFiles(
            directory,
            FileSource(),
            UpdateApkVerifier { _, _ ->
                throw UpdateVerificationException()
            },
            StandardTestDispatcher(testScheduler),
        )
        expectIo { files.download(RELEASE, {}, {}) }
        assertFalse(File(directory, "update.apk").exists())
        assertFalse(File(directory, "update.apk.part").exists())
    }

    @Test fun cancellationDeletesPartialAndStartupCleansOrphans() = runTest {
        val directory = temporary.newFolder()
        val source = FileSource().apply { wait = true }
        val files = PrivateAppUpdateFiles(
            directory,
            source,
            UpdateApkVerifier { _, _ -> },
            StandardTestDispatcher(testScheduler),
        )
        val pending = launch { files.download(RELEASE, {}, {}) }
        runCurrent()
        assertTrue(File(directory, "update.apk.part").exists())
        pending.cancelAndJoin()
        assertFalse(File(directory, "update.apk.part").exists())
        File(directory, "update.apk.part").writeText("interrupted")
        File(directory, "update.apk").writeText("orphan")
        assertFalse(files.restore(null, null))
        assertTrue(directory.listFiles()!!.isEmpty())
    }

    private suspend fun expectIo(block: suspend () -> Unit) {
        var failed = false
        try {
            block()
        } catch (_: IOException) {
            failed = true
        }
        assertTrue(failed)
    }

    private class FileSource : AppReleaseSource {
        var body = "book"
        var wait = false
        override suspend fun latest(): AppRelease = RELEASE
        override suspend fun checksum(release: AppRelease): String = HASH
        override suspend fun download(release: AppRelease, target: File, progress: (Int) -> Unit) {
            target.writeText(body)
            if (wait) awaitCancellation()
        }
    }

    private companion object {
        val HASH = MessageDigest.getInstance("SHA-256").digest("book".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val RELEASE = AppRelease("2026.11.0", "", "2026-11-10T00:00:00Z", "apk", "sum", 4)
    }
}
