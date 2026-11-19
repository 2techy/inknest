package dev.twotechy.inknest.data.source.updates

import dev.twotechy.inknest.model.AppRelease
import java.io.File

internal interface AppReleaseDataSource {
    suspend fun latest(): AppRelease
    suspend fun checksum(release: AppRelease): String
    suspend fun download(release: AppRelease, target: File, progress: (Int) -> Unit)
}
