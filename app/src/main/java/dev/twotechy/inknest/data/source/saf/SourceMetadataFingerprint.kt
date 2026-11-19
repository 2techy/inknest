package dev.twotechy.inknest.data.source.saf

import java.io.IOException

internal data class SourceMetadataFingerprint(val documents: Map<Document, SourceDocument?>) {
    data class Document(val tree: String, val path: List<String>)

    fun reusableAt(now: Long): Boolean = documents.isNotEmpty() &&
        documents.values.all { it.reusableAt(now) }

    private fun SourceDocument?.reusableAt(now: Long): Boolean {
        if (this == null) return true
        val bytes = size ?: return false
        val timestamp = modified ?: return false
        if (bytes < 0) return false
        return timestamp > 0 && timestamp <= now - 5_000
    }

    suspend fun matches(reader: SourceDocumentReader, now: Long): Boolean {
        if (!reusableAt(now)) return false
        for ((reference, expected) in documents) {
            val actual = try {
                reader.describe(reference.tree, reference.path)
            } catch (_: MissingSourceFileException) {
                null
            } catch (_: IOException) {
                return false
            } catch (_: SecurityException) {
                return false
            }
            if (actual != expected) return false
        }
        return true
    }
}
