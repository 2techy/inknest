package dev.twotechy.inknest.data.source.books

import dev.twotechy.inknest.data.source.saf.fingerprint
import dev.twotechy.inknest.model.ReadingSourceConfig

internal data class BookChecksum(val tree: String, val fingerprint: String?, val checksum: String)

internal fun interface BookChecksumSource {
    suspend fun checksum(
        config: ReadingSourceConfig,
        path: String,
        cached: suspend (String, String) -> String?,
    ): BookChecksum
}
