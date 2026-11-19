package dev.twotechy.inknest.platform.updates

import dev.twotechy.inknest.model.AppRelease
import java.io.File

internal fun interface UpdateApkVerifier {
    suspend fun verify(file: File, release: AppRelease)
}
