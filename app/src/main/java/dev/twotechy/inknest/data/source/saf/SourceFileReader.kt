package dev.twotechy.inknest.data.source.saf

import java.io.IOException
import java.io.InputStream

internal class MissingSourceFile(cause: Throwable? = null) :
    IOException("Source file is unavailable", cause)

internal interface SourceFileReader {
    suspend fun <T> read(tree: String, path: List<String>, parse: suspend (InputStream) -> T): T
}

internal interface SourceDocumentReader : SourceFileReader {
    suspend fun describe(tree: String, path: List<String>): SourceDocument
}

internal interface DirectSourceDocumentReader : SourceDocumentReader {
    suspend fun <T> readDocument(document: SourceDocument, parse: suspend (InputStream) -> T): T
}

internal fun SourceDocument.fingerprint(): String? {
    val size = size ?: return null
    val modified = modified ?: return null
    if (modified <= 0) return null
    return "$size:$modified"
}
