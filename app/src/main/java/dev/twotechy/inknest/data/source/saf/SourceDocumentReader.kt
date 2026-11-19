package dev.twotechy.inknest.data.source.saf

internal interface SourceDocumentReader : SourceFileReader {
    suspend fun describe(tree: String, path: List<String>): SourceDocument
}
