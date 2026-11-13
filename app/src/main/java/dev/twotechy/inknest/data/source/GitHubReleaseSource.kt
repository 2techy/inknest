package dev.twotechy.inknest.data.source

import dev.twotechy.inknest.model.AppRelease
import dev.twotechy.inknest.model.CalVer
import java.io.File
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

internal interface AppReleaseSource {
    suspend fun latest(): AppRelease
    suspend fun checksum(release: AppRelease): String
    suspend fun download(release: AppRelease, target: File, progress: (Int) -> Unit)
}

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

internal object AppReleaseJson {
    fun decode(raw: String): AppRelease = try {
        parseRelease(JSONObject(raw))
    } catch (invalid: JSONException) {
        throw IOException("Invalid release metadata", invalid)
    } catch (invalid: IllegalArgumentException) {
        throw IOException("Invalid release metadata", invalid)
    }

    private fun stableVersion(json: JSONObject): String {
        if (json.getBoolean("draft") || json.getBoolean("prerelease")) {
            throw IOException("Not a stable release")
        }
        val version = json.getString("tag_name").removePrefix("v")
        if (CalVer.parse(version) == null || json.getString("tag_name") != "v$version") {
            throw IOException("Invalid release version")
        }
        return version
    }

    private fun parseRelease(json: JSONObject): AppRelease {
        val version = stableVersion(json)
        val assets = json.getJSONArray("assets")
        val named = (0 until assets.length()).map(assets::getJSONObject)
        val apk = requiredAsset(named, "InkNest-$version.apk")
        val sums = requiredAsset(named, "SHA256SUMS.txt")
        val size = apk.getLong("size")
        if (size !in 1..MAX_UPDATE_APK_BYTES) throw IOException("APK size out of bounds")
        return AppRelease(
            version,
            json.optString("body", "").take(100_000),
            json.getString("published_at"),
            assetUrl(apk, version),
            assetUrl(sums, version),
            size,
        )
    }

    private fun requiredAsset(assets: List<JSONObject>, name: String): JSONObject =
        assets.singleOrNull { it.getString("name") == name }
            ?: throw IOException("Missing or ambiguous asset")

    fun encode(release: AppRelease): String = JSONObject().apply {
        put("draft", false)
        put("prerelease", false)
        put("tag_name", "v${release.version}")
        put("body", release.notes)
        put("published_at", release.publishedAt)
        put(
            "assets",
            org.json.JSONArray().put(asset(release.apkName, release.apkUrl, release.size))
                .put(asset("SHA256SUMS.txt", release.checksumUrl, 0)),
        )
    }.toString()

    private fun asset(name: String, url: String, size: Long): JSONObject = JSONObject().apply {
        put("name", name)
        put("browser_download_url", url)
        put("size", size)
    }

    private fun assetUrl(asset: JSONObject, version: String): String {
        val url = asset.getString("browser_download_url")
        val expected = "https://github.com/2techy/inknest/releases/download/v$version/" +
            asset.getString("name")
        if (URI.create(url).toASCIIString() != expected) throw IOException("Unexpected asset URL")
        return url
    }
}

internal const val MAX_UPDATE_APK_BYTES = 128L * 1024 * 1024
