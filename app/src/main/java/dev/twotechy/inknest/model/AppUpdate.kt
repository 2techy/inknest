package dev.twotechy.inknest.model

internal data class CalVer(val year: Int, val month: Int, val patch: Int) : Comparable<CalVer> {
    override fun compareTo(other: CalVer): Int =
        compareValuesBy(this, other, CalVer::year, CalVer::month, CalVer::patch)

    companion object {
        fun parse(value: String): CalVer? {
            val match = Regex("(\\d{4})\\.(\\d{1,2})\\.(0|[1-9]\\d*)").matchEntire(value)
                ?: return null
            val (year, month, patch) = match.destructured
            val result = CalVer(year.toInt(), month.toInt(), patch.toIntOrNull() ?: return null)
            return result.takeIf { it.year > 0 && it.month in 1..12 }
        }
    }
}

internal data class AppRelease(
    val version: String,
    val notes: String,
    val publishedAt: String,
    val apkUrl: String,
    val checksumUrl: String,
    val size: Long,
) {
    val apkName: String get() = "InkNest-$version.apk"
    val pageUrl: String get() = "https://github.com/2techy/inknest/releases/tag/v$version"
}

internal data class UpdateSnapshot(
    val automatic: Boolean = true,
    val attemptedAt: Long? = null,
    val checkedAt: Long? = null,
    val retryAt: Long? = null,
    val release: AppRelease? = null,
    val downloadedSha256: String? = null,
)

internal enum class UpdateDownloadStage { Idle, Downloading, Verifying, Ready }
internal enum class AppUpdateError { Check, Download, Verify, Storage, RateLimit }

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
