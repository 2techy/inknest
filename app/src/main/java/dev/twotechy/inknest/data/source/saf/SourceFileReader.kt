package dev.twotechy.inknest.data.source.saf

import java.io.InputStream

internal interface SourceFileReader {
    suspend fun <T> read(tree: String, path: List<String>, parse: suspend (InputStream) -> T): T
}

internal fun SourceDocument.fingerprint(): String? {
    val size = size ?: return null
    val modified = modified ?: return null
    if (modified <= 0) return null
    return "$size:$modified"
}
