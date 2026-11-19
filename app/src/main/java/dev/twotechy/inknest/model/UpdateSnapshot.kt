package dev.twotechy.inknest.model

internal data class UpdateSnapshot(
    val automatic: Boolean = true,
    val attemptedAt: Long? = null,
    val checkedAt: Long? = null,
    val retryAt: Long? = null,
    val release: AppRelease? = null,
    val downloadedSha256: String? = null,
)
