package dev.twotechy.inknest.platform.updates

import dev.twotechy.inknest.model.AppRelease

internal interface AppUpdateFiles {
    suspend fun restore(release: AppRelease?, sha256: String?): Boolean
    suspend fun download(
        release: AppRelease,
        progress: (Int) -> Unit,
        verifying: () -> Unit,
    ): String
    suspend fun prepare(release: AppRelease, sha256: String): String
    suspend fun clear()
}
