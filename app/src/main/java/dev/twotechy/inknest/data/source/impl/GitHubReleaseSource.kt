package dev.twotechy.inknest.data.source.impl

import dev.twotechy.inknest.data.source.AppReleaseJson
import dev.twotechy.inknest.data.source.AppReleaseSource
import dev.twotechy.inknest.data.source.UpdateHttpTransport
import dev.twotechy.inknest.model.AppRelease
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class GitHubReleaseSource(
    private val http: UpdateHttpTransport,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AppReleaseSource {
    override suspend fun latest(): AppRelease = withContext(dispatcher) {
        AppReleaseJson.decode(
            http.text("https://api.github.com/repos/2techy/inknest/releases/latest", 1_048_576),
        )
    }

    override suspend fun checksum(release: AppRelease): String = withContext(dispatcher) {
        val entries = http.text(release.checksumUrl, 65_536).lineSequence().mapNotNull { line ->
            Regex(
                "([a-fA-F0-9]{64}) [ *](.+)",
            ).matchEntire(line)?.destructured?.let { (hash, name) ->
                if (name == release.apkName) hash.lowercase() else null
            }
        }.toList()
        entries.singleOrNull() ?: throw IOException("Missing or ambiguous APK checksum")
    }

    override suspend fun download(release: AppRelease, target: File, progress: (Int) -> Unit) {
        http.download(release.apkUrl, target, release.size, progress)
    }
}
