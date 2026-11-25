package dev.twotechy.inknest.data.local.database.books

import java.security.MessageDigest

internal fun bookFileId(
    sourceId: String,
    path: String,
    fingerprint: String?,
    fallbackPartition: String,
): String {
    val revision = fingerprint?.let { "known:$it" } ?: "unknown:$fallbackPartition"
    val key = buildString {
        append(sourceId.length).append(':').append(sourceId)
        append(path.length).append(':').append(path)
        append(revision)
    }
    val digest = MessageDigest.getInstance("SHA-256").digest(key.encodeToByteArray())
    return "file:${digest.toHexString()}"
}
