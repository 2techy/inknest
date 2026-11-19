package dev.twotechy.inknest.model

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
