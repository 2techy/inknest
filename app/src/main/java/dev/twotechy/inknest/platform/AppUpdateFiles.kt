package dev.twotechy.inknest.platform

import dev.twotechy.inknest.data.source.AppReleaseSource
import dev.twotechy.inknest.model.AppRelease
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

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

internal fun interface UpdateApkVerifier {
    suspend fun verify(file: File, release: AppRelease)
}

internal class PrivateAppUpdateFiles(
    private val directory: File,
    private val source: AppReleaseSource,
    private val verifier: UpdateApkVerifier,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AppUpdateFiles {
    private val partial get() = File(directory, "update.apk.part")
    private val apk get() = File(directory, "update.apk")

    override suspend fun restore(release: AppRelease?, sha256: String?): Boolean {
        return withContext(dispatcher) {
            remove(partial)
            if (release == null || sha256 == null) {
                remove(apk)
                return@withContext false
            }
            if (!apk.exists()) return@withContext false
            try {
                prepare(release, sha256)
                true
            } catch (_: IOException) {
                remove(apk)
                false
            }
        }
    }

    override suspend fun download(
        release: AppRelease,
        progress: (Int) -> Unit,
        verifying: () -> Unit,
    ): String = withContext(dispatcher) {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("No update directory")
        if (directory.usableSpace <
            release.size + 1_048_576
        ) {
            throw IOException("Insufficient storage")
        }
        val checksum = source.checksum(release)
        try {
            source.download(release, partial, progress)
            verifying()
            verify(partial, release, checksum)
            remove(apk)
            if (!partial.renameTo(apk)) throw IOException("Cannot retain update")
            checksum
        } finally {
            withContext(NonCancellable + dispatcher) { remove(partial) }
        }
    }

    override suspend fun prepare(release: AppRelease, sha256: String): String =
        withContext(dispatcher) {
            verify(apk, release, sha256)
            apk.name
        }

    override suspend fun clear() {
        withContext(dispatcher) {
            remove(partial)
            remove(apk)
        }
    }

    private suspend fun verify(file: File, release: AppRelease, expected: String) {
        if (!file.isFile || file.length() != release.size) throw UpdateVerificationException()
        if (!Regex("[a-f0-9]{64}").matches(expected)) throw UpdateVerificationException()
        if (sha256(file) != expected) throw UpdateVerificationException()
        verifier.verify(file, release)
    }

    private suspend fun sha256(file: File): String {
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(16_384)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                hash.update(buffer, 0, count)
            }
        }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }

    private fun remove(file: File) {
        if (file.exists() && !file.delete()) throw IOException("Cannot remove update file")
    }
}
