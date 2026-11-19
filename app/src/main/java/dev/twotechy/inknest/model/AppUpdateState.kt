package dev.twotechy.inknest.model

internal data class AppUpdateState(
    val loaded: Boolean = false,
    val canCompareVersion: Boolean = false,
    val automaticAllowed: Boolean = false,
    val snapshot: UpdateSnapshot = UpdateSnapshot(),
    val available: Boolean = false,
    val checking: Boolean = false,
    val download: UpdateDownloadStage = UpdateDownloadStage.Idle,
    val progress: Int = 0,
    val error: AppUpdateError? = null,
) {
    val canDownload: Boolean get() {
        if (snapshot.release == null) return false
        return available || !canCompareVersion
    }
    val busy: Boolean get() = checking || download == UpdateDownloadStage.Downloading ||
        download == UpdateDownloadStage.Verifying
}
