package dev.twotechy.inknest.data.repository

import androidx.lifecycle.ViewModelStore
import dev.twotechy.inknest.data.local.AppUpdateStore
import dev.twotechy.inknest.data.source.AppReleaseSource
import dev.twotechy.inknest.data.source.UpdateRateLimitException
import dev.twotechy.inknest.model.AppRelease
import dev.twotechy.inknest.model.AppUpdateError
import dev.twotechy.inknest.model.UpdateDownloadStage
import dev.twotechy.inknest.model.UpdateSnapshot
import dev.twotechy.inknest.platform.AppUpdateFiles
import dev.twotechy.inknest.ui.settings.SettingsViewModel
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateRepositoryTest {
    @Test fun settingsEntryChecksWhenUncachedButWaitsForSavedResultsBeforeDeciding() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val models = ViewModelStore()
        try {
            val fresh = fixture(automaticAllowed = false)
            val first = SettingsViewModel(
                TestUserPreferencesRepository(),
                UnconnectedSourceSettingsRepository(),
                AcknowledgementsRepository { "" },
                updates = fresh.repository,
            )
            models.put("first", first)
            first.openUpdates()
            runCurrent()
            assertEquals(1, fresh.source.calls)
            first.openUpdates()
            runCurrent()
            assertEquals(1, fresh.source.calls)

            val saved = fixture(
                store = FakeUpdateStore(UpdateSnapshot(checkedAt = NOW, release = RELEASE)),
                automaticAllowed = false,
            )
            val cached = SettingsViewModel(
                TestUserPreferencesRepository(),
                UnconnectedSourceSettingsRepository(),
                AcknowledgementsRepository { "" },
                updates = saved.repository,
            )
            models.put("cached", cached)
            // The repository has not emitted its first persisted snapshot yet.
            cached.openUpdates()
            runCurrent()
            assertEquals(0, saved.source.calls)
        } finally {
            models.clear()
            Dispatchers.resetMain()
        }
    }

    @Test fun automaticChecksOncePerDayAcrossForegroundSessionsAndCanBeDisabled() = runTest {
        val fixture = fixture()
        var foreground = backgroundScope.launch { fixture.repository.runForeground() }
        runCurrent()
        assertEquals(1, fixture.source.calls)
        foreground.cancelAndJoin()
        advanceTimeBy(23 * HOUR)
        foreground = backgroundScope.launch { fixture.repository.runForeground() }
        runCurrent()
        assertEquals(1, fixture.source.calls)
        advanceTimeBy(HOUR)
        runCurrent()
        assertEquals(2, fixture.source.calls)
        fixture.repository.setAutomatic(false)
        runCurrent()
        advanceTimeBy(24 * HOUR)
        runCurrent()
        assertEquals(2, fixture.source.calls)
        fixture.repository.check()
        runCurrent()
        assertEquals(3, fixture.source.calls)
        foreground.cancelAndJoin()
    }

    @Test fun failuresRetainSnapshotAndManualChecksBypassDailyThrottle() = runTest {
        val fixture = fixture()
        runCurrent()
        fixture.repository.check()
        runCurrent()
        val successful = fixture.repository.state.value.snapshot
        fixture.source.failure = IOException("offline")
        fixture.repository.check()
        runCurrent()
        assertEquals(2, fixture.source.calls)
        val state = fixture.repository.state.value
        assertEquals(successful.release, state.snapshot.release)
        assertEquals(successful.checkedAt, state.snapshot.checkedAt)
        assertTrue(state.available)
        assertEquals(AppUpdateError.Check, state.error)
        fixture.source.failure = null
        fixture.repository.check()
        assertNull(fixture.repository.state.value.error)
    }

    @Test fun concurrentChecksAreDeduplicatedAndCancellationAllowsRetry() = runTest {
        val fixture = fixture()
        fixture.source.pending = CompletableDeferred()
        val first = launch { fixture.repository.check() }
        runCurrent()
        fixture.repository.check()
        assertEquals(1, fixture.source.calls)
        first.cancelAndJoin()
        assertFalse(fixture.repository.state.value.checking)
        assertNull(fixture.repository.state.value.error)
        fixture.source.pending = null
        fixture.repository.check()
        runCurrent()
        assertEquals(2, fixture.source.calls)
        assertTrue(fixture.repository.state.value.available)
    }

    @Test fun failedWritesDoNotReportSuccessOrChangeSavedPreferences() = runTest {
        val fixture = fixture()
        runCurrent()
        fixture.store.fail = true
        fixture.repository.setAutomatic(false)
        assertTrue(fixture.repository.state.value.snapshot.automatic)
        assertEquals(AppUpdateError.Storage, fixture.repository.state.value.error)
        fixture.repository.check()
        assertEquals(0, fixture.source.calls)
        assertNull(fixture.repository.state.value.snapshot.checkedAt)
        fixture.store.fail = false
        fixture.repository.check()
        runCurrent()
        assertTrue(fixture.repository.state.value.available)
    }

    @Test fun rateLimitSurvivesRestartAndAppliesToManualChecks() = runTest {
        val fixture = fixture()
        runCurrent()
        fixture.source.failure = UpdateRateLimitException(NOW + HOUR)
        fixture.repository.check()
        runCurrent()
        val restarted = fixture(store = fixture.store)
        runCurrent()
        restarted.repository.check()
        assertEquals(0, restarted.source.calls)
        assertEquals(AppUpdateError.RateLimit, restarted.repository.state.value.error)
        advanceTimeBy(HOUR)
        restarted.repository.check()
        runCurrent()
        assertEquals(1, restarted.source.calls)
    }

    @Test fun debugBuildDoesNotAutoCheckAndManualUpdatesUseSharedDownloadFlow() = runTest {
        val debug = fixture(automaticAllowed = false)
        backgroundScope.launch { debug.repository.runForeground() }
        runCurrent()
        assertEquals(0, debug.source.calls)
        debug.repository.check()
        assertEquals(1, debug.source.calls)
        val local = fixture(version = "0.1.0-dev")
        backgroundScope.launch { local.repository.runForeground() }
        runCurrent()
        assertEquals(0, local.source.calls)
        local.repository.check()
        runCurrent()
        local.repository.download()
        runCurrent()
        assertFalse(local.repository.state.value.canCompareVersion)
        assertEquals(1, local.source.calls)
        assertEquals(RELEASE, local.repository.state.value.snapshot.release)
        assertFalse(local.repository.state.value.available)
        assertEquals(1, local.files.downloads)
        assertEquals(UpdateDownloadStage.Ready, local.repository.state.value.download)
        assertEquals("update.apk", local.repository.prepareInstall())
    }

    @Test fun localBuildManualCheckReportsLoadingAndErrorsInsteadOfReturningSilently() = runTest {
        val local = fixture(version = "0.1.0-dev", automaticAllowed = false)
        local.source.pending = CompletableDeferred()
        val check = launch { local.repository.check() }
        runCurrent()
        assertTrue(local.repository.state.value.checking)
        local.source.pending!!.complete(Unit)
        check.join()
        runCurrent()
        assertFalse(local.repository.state.value.checking)
        assertEquals(RELEASE, local.repository.state.value.snapshot.release)
        assertFalse(local.repository.state.value.automaticAllowed)
        local.source.failure = IOException("offline")
        local.repository.check()
        assertEquals(AppUpdateError.Check, local.repository.state.value.error)
        assertEquals(RELEASE, local.repository.state.value.snapshot.release)
    }

    @Test fun backgroundCancelsDownloadAndForegroundRetryCanBeInstalledRepeatedly() = runTest {
        val fixture = fixture()
        val foreground = backgroundScope.launch { fixture.repository.runForeground() }
        runCurrent()
        fixture.files.waitForCancellation = true
        fixture.repository.download()
        fixture.repository.download()
        runCurrent()
        assertEquals(1, fixture.files.downloads)
        foreground.cancelAndJoin()
        runCurrent()
        assertTrue(fixture.files.cancelled)
        assertEquals(UpdateDownloadStage.Idle, fixture.repository.state.value.download)
        fixture.repository.download()
        runCurrent()
        assertEquals(1, fixture.files.downloads)
        backgroundScope.launch { fixture.repository.runForeground() }
        runCurrent()
        fixture.files.waitForCancellation = false
        fixture.repository.download()
        runCurrent()
        assertEquals(UpdateDownloadStage.Ready, fixture.repository.state.value.download)
        assertEquals("update.apk", fixture.repository.prepareInstall())
        // Cancelling the Android installer leaves the verified APK available for another attempt.
        assertEquals("update.apk", fixture.repository.prepareInstall())
    }

    @Test fun restoresCompletedDownloadButDropsItAfterApplicationUpgrade() = runTest {
        val store = FakeUpdateStore(UpdateSnapshot(release = RELEASE, downloadedSha256 = HASH))
        val restored = fixture(store = store)
        restored.files.restorable = true
        runCurrent()
        assertEquals(UpdateDownloadStage.Ready, restored.repository.state.value.download)
        val upgraded = fixture(store = store, version = RELEASE.version)
        runCurrent()
        assertFalse(upgraded.repository.state.value.available)
        assertNull(store.snapshots.value.downloadedSha256)
        assertEquals(UpdateDownloadStage.Idle, upgraded.repository.state.value.download)
    }

    @Test fun completedDownloadRestoresWithoutSpecialBuildFallbacks() = runTest {
        val saved = FakeUpdateStore(UpdateSnapshot(release = RELEASE, downloadedSha256 = HASH))
        val fixture = fixture(store = saved, version = "0.1.0-dev", automaticAllowed = false)
        fixture.files.restorable = true
        runCurrent()
        assertFalse(fixture.repository.state.value.available)
        assertTrue(fixture.repository.state.value.canDownload)
        assertEquals(UpdateDownloadStage.Ready, fixture.repository.state.value.download)
        assertEquals("update.apk", fixture.repository.prepareInstall())
    }

    @Test fun equalOrOlderReleaseDoesNotOfferDowngrade() = runTest {
        val fixture = fixture(version = "2027.1.0")
        runCurrent()
        fixture.repository.check()
        runCurrent()
        assertFalse(fixture.repository.state.value.available)
        assertFalse(fixture.repository.state.value.canDownload)
    }

    private fun TestScope.fixture(
        store: FakeUpdateStore = FakeUpdateStore(),
        version: String = "2026.10.0",
        automaticAllowed: Boolean = true,
    ): Fixture {
        val source = FakeReleaseSource()
        val files = FakeUpdateFiles()
        val clock = object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = Instant.ofEpochMilli(millis())
            override fun millis(): Long = NOW + testScheduler.currentTime
        }
        return Fixture(
            AppUpdateRepository(
                source,
                store,
                files,
                backgroundScope,
                version,
                automaticAllowed,
                clock,
            ),
            source,
            store,
            files,
        )
    }

    private data class Fixture(
        val repository: AppUpdateRepository,
        val source: FakeReleaseSource,
        val store: FakeUpdateStore,
        val files: FakeUpdateFiles,
    )

    private class FakeUpdateStore(initial: UpdateSnapshot = UpdateSnapshot()) : AppUpdateStore {
        override val snapshots = MutableStateFlow(initial)
        var fail = false
        override suspend fun update(transform: (UpdateSnapshot) -> UpdateSnapshot) {
            if (fail) throw IOException("disk full")
            snapshots.value = transform(snapshots.value)
        }
    }

    private class FakeReleaseSource : AppReleaseSource {
        var calls = 0
        var failure: IOException? = null
        var pending: CompletableDeferred<Unit>? = null
        override suspend fun latest(): AppRelease {
            calls++
            pending?.await()
            failure?.let { throw it }
            return RELEASE
        }
        override suspend fun checksum(release: AppRelease): String = HASH
        override suspend fun download(release: AppRelease, target: File, progress: (Int) -> Unit) =
            Unit
    }

    private class FakeUpdateFiles : AppUpdateFiles {
        var downloads = 0
        var restorable = false
        var waitForCancellation = false
        var cancelled = false
        override suspend fun restore(release: AppRelease?, sha256: String?): Boolean =
            restorable && release != null && sha256 != null
        override suspend fun download(
            release: AppRelease,
            progress: (Int) -> Unit,
            verifying: () -> Unit,
        ): String {
            downloads++
            if (waitForCancellation) {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
            progress(100)
            verifying()
            return HASH
        }
        override suspend fun prepare(release: AppRelease, sha256: String): String = "update.apk"
        override suspend fun clear() = Unit
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val NOW = 1_799_000_000_000L
        val HASH = "a".repeat(64)
        val RELEASE = AppRelease("2026.11.0", "Notes", "2026-11-10T00:00:00Z", "apk", "sum", 4)
    }
}
