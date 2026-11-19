package dev.twotechy.inknest.data.source.saf

import java.io.InputStream

internal interface DirectSourceDocumentReader : SourceDocumentReader {
    suspend fun <T> readDocument(document: SourceDocument, parse: suspend (InputStream) -> T): T
}
