package dev.twotechy.inknest.data.source.books

import dev.twotechy.inknest.model.ReadingSourceConfig

internal fun interface BookChecksumDataSource {
    suspend fun checksum(
        config: ReadingSourceConfig,
        path: String,
        cached: suspend (String, String) -> String?,
    ): BookChecksum
}
