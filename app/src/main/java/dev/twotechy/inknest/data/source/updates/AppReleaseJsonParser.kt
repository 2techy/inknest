package dev.twotechy.inknest.data.source.updates

import dev.twotechy.inknest.model.AppRelease
import dev.twotechy.inknest.model.CalVer
import java.io.IOException
import java.net.URI
import org.json.JSONException
import org.json.JSONObject

internal object AppReleaseJsonParser {
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
