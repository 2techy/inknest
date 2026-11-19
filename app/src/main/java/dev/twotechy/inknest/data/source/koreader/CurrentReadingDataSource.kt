package dev.twotechy.inknest.data.source.koreader

import dev.twotechy.inknest.data.source.saf.SourceMetadataFingerprint
import dev.twotechy.inknest.model.ReadingSourceConfig

internal fun interface CurrentReadingDataSource {
    suspend fun read(config: ReadingSourceConfig): CurrentReadingSnapshot

    suspend fun unchanged(
        config: ReadingSourceConfig,
        fingerprint: SourceMetadataFingerprint,
    ): Boolean = false
}
