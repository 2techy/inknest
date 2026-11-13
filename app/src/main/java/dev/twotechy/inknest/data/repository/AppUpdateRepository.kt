package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.local.AppUpdateStore
import dev.twotechy.inknest.data.source.AppReleaseSource
import dev.twotechy.inknest.data.source.UpdateRateLimitException
import dev.twotechy.inknest.model.AppRelease
import dev.twotechy.inknest.model.AppUpdateError
import dev.twotechy.inknest.model.AppUpdateState
import dev.twotechy.inknest.model.CalVer
import dev.twotechy.inknest.model.UpdateDownloadStage
import dev.twotechy.inknest.model.UpdateSnapshot
import dev.twotechy.inknest.platform.AppUpdateFiles
import dev.twotechy.inknest.platform.UpdateVerificationException
import java.io.IOException
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

internal class AppUpdateRepository(
    private val source: AppReleaseSource,
    private val store: AppUpdateStore,
    private val files: AppUpdateFiles,
    private val scope: CoroutineScope,
    versionName: String,
    private val automaticAllowed: Boolean,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val currentVersion = CalVer.parse(versionName)
    private val mutableState = MutableStateFlow(
        AppUpdateState(
            canCompareVersion = currentVersion != null,
            automaticAllowed = automaticAllowed && currentVersion != null,
        ),
    )
    val state = mutableState.asStateFlow()
    private val operation = Mutex()
    private var downloadJob: Job? = null

    @Volatile private var foreground = false

    init {
        scope.launch {
            store.snapshots.recoverObservation(
                ObservationRetry(),
                onError = { fail(AppUpdateError.Storage) },
            ).collect { saved ->
                if (!state.value.loaded) restore(saved)
                mutableState.update {
                    it.copy(loaded = true, snapshot = saved, available = newer(saved.release))
                }
            }
        }
    }

    suspend fun runForeground() {
        foreground = true
        try {
            state.first { it.loaded }
            state.map { it.snapshot.automatic }.distinctUntilChanged().collectLatest { enabled ->
                if (!automaticEnabled(enabled)) awaitCancellation()
                while (true) {
                    check(manual = false)
                    val attempted = state.value.snapshot.attemptedAt ?: clock.millis()
                    val next = maxOf(attempted + CHECK_INTERVAL, state.value.snapshot.retryAt ?: 0)
                    delay((next - clock.millis()).coerceIn(60_000, CHECK_INTERVAL))
                }
            }
        } finally {
            foreground = false
            cancelDownload()
        }
    }

    suspend fun setAutomatic(enabled: Boolean) {
        try {
            persist { it.copy(automatic = enabled) }
            mutableState.update { it.copy(error = null) }
        } catch (_: IOException) {
            fail(AppUpdateError.Storage)
        }
    }

    suspend fun check(manual: Boolean = true) {
        state.first { it.loaded }
        if (!operation.tryLock()) return
        try {
            if (!shouldCheck(manual)) return
            recover(AppUpdateError.Check) { fetchRelease() }
        } finally {
            mutableState.update { it.copy(checking = false) }
            operation.unlock()
        }
    }

    fun download() {
        if (!foreground || downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            if (!foreground) return@launch
            val release = state.value.snapshot.release ?: return@launch
            if (!state.value.canDownload || !operation.tryLock()) return@launch
            try {
                recover(AppUpdateError.Download) { downloadRelease(release) }
            } finally {
                mutableState.update {
                    if (it.download == UpdateDownloadStage.Ready) {
                        it
                    } else {
                        it.copy(download = UpdateDownloadStage.Idle, progress = 0)
                    }
                }
                operation.unlock()
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    suspend fun prepareInstall(): String? {
        if (!operation.tryLock()) return null
        try {
            val (release, checksum) = installCandidate() ?: return null
            mutableState.update { it.copy(download = UpdateDownloadStage.Verifying, error = null) }
            val token = recover(AppUpdateError.Verify) { files.prepare(release, checksum) }
            val stage = if (token == null) UpdateDownloadStage.Idle else UpdateDownloadStage.Ready
            mutableState.update { it.copy(download = stage) }
            return token
        } catch (cancelled: CancellationException) {
            mutableState.update { it.copy(download = UpdateDownloadStage.Ready) }
            throw cancelled
        } finally {
            operation.unlock()
        }
    }

    private suspend fun restore(saved: UpdateSnapshot) {
        try {
            val candidate = saved.release?.takeIf { currentVersion == null || newer(it) }
            val valid = files.restore(candidate, saved.downloadedSha256)
            if (valid) mutableState.update { it.copy(download = UpdateDownloadStage.Ready) }
            if (!valid && saved.downloadedSha256 != null) {
                persist { it.copy(downloadedSha256 = null) }
            }
        } catch (_: IOException) {
            fail(AppUpdateError.Storage)
        }
    }

    private suspend fun saveRelease(release: AppRelease) {
        val previous = state.value.snapshot.release
        val changed = previous != release
        if (changed) {
            mutableState.update { it.copy(download = UpdateDownloadStage.Idle) }
            files.clear()
        }
        persist {
            it.copy(
                checkedAt = clock.millis(),
                retryAt = null,
                release = release,
                downloadedSha256 = if (changed) null else it.downloadedSha256,
            )
        }
    }

    private fun automaticEnabled(enabled: Boolean): Boolean =
        enabled && automaticAllowed && currentVersion != null

    private fun shouldCheck(manual: Boolean): Boolean {
        val saved = state.value.snapshot
        val now = clock.millis()
        if (saved.retryAt?.let { it > now } == true) {
            if (manual) fail(AppUpdateError.RateLimit)
            return false
        }
        if (manual) return true
        return automaticEnabled(saved.automatic) && due(saved.attemptedAt, now)
    }

    private suspend fun fetchRelease() {
        mutableState.update { it.copy(checking = true, error = null) }
        persist { it.copy(attemptedAt = clock.millis()) }
        val release = withTimeoutOrNull(30_000) { source.latest() }
            ?: throw IOException("Update check timed out")
        saveRelease(release)
    }

    private suspend fun downloadRelease(release: AppRelease) {
        mutableState.update {
            it.copy(download = UpdateDownloadStage.Downloading, progress = 0, error = null)
        }
        val checksum = withTimeoutOrNull(15 * 60_000L) {
            files.download(
                release,
                progress = { percent -> mutableState.update { it.copy(progress = percent) } },
                verifying = {
                    mutableState.update { it.copy(download = UpdateDownloadStage.Verifying) }
                },
            )
        } ?: throw IOException("Download timed out")
        persist { it.copy(downloadedSha256 = checksum) }
        mutableState.update { it.copy(download = UpdateDownloadStage.Ready) }
    }

    private fun installCandidate(): Pair<AppRelease, String>? {
        val current = state.value
        if (!current.canDownload || current.download != UpdateDownloadStage.Ready) return null
        val release = current.snapshot.release ?: return null
        val checksum = current.snapshot.downloadedSha256 ?: return null
        return release to checksum
    }

    private suspend fun <T> recover(fallback: AppUpdateError, block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: UpdateStorageException) {
        fail(AppUpdateError.Storage)
        null
    } catch (_: UpdateVerificationException) {
        fail(AppUpdateError.Verify)
        null
    } catch (limited: UpdateRateLimitException) {
        recordRateLimit(limited.retryAt)
        null
    } catch (_: IOException) {
        fail(fallback)
        null
    }

    private suspend fun recordRateLimit(retryAt: Long) {
        try {
            persist { it.copy(retryAt = retryAt) }
            fail(AppUpdateError.RateLimit)
        } catch (_: IOException) {
            fail(AppUpdateError.Storage)
        }
    }

    private suspend fun persist(transform: (UpdateSnapshot) -> UpdateSnapshot) {
        try {
            store.update(transform)
        } catch (error: IOException) {
            throw UpdateStorageException(error)
        }
    }

    private fun newer(release: AppRelease?): Boolean {
        val remote = release?.version?.let(CalVer::parse) ?: return false
        return currentVersion?.let { remote > it } ?: false
    }

    private fun fail(error: AppUpdateError) = mutableState.update { it.copy(error = error) }

    private fun due(attempt: Long?, now: Long): Boolean =
        attempt == null || now < attempt || now - attempt >= CHECK_INTERVAL

    private companion object {
        const val CHECK_INTERVAL = 24 * 60 * 60_000L
    }
}

private class UpdateStorageException(cause: IOException) : IOException(cause)
