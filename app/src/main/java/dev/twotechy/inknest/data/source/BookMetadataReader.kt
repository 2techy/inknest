package dev.twotechy.inknest.data.source

import dev.twotechy.inknest.data.source.ProbeRead
import dev.twotechy.inknest.data.source.impl.BookProbeDataSource
import dev.twotechy.inknest.data.source.readCapability
import dev.twotechy.inknest.model.ReadingItem
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.model.ReadingStatus
import java.util.UUID
import kotlin.math.roundToInt

internal data class BookMetadata(
    val fingerprint: String?,
    val title: String,
    val author: String?,
    val coverUri: String?,
    val description: String? = null,
)
internal data class EnrichedReadingItem(val item: ReadingItem, val metadata: BookMetadata?)

internal class BookMetadataReader(
    private val books: BookProbeDataSource,
    private val covers: BookCoverCache,
    private val previous: suspend (ReadingSourceConfig, String) -> BookMetadata?,
) {
    suspend fun read(
        config: ReadingSourceConfig,
        book: ResolvedBook,
        item: ReadingItem,
    ): EnrichedReadingItem {
        val sidecar = (readCapability { books.sidecar(book) }.result as? ProbeRead.Success)?.value
        val metadata = metadata(config, book, item)
        return EnrichedReadingItem(
            item.copy(
                title = metadata.title,
                description = metadata.description,
                author = metadata.author,
                coverUri = metadata.coverUri,
                progress = sidecar?.percent?.let { (it * 100).roundToInt() },
                status = readingStatus(sidecar?.status),
                currentPage = sidecar?.currentPage,
                totalPages = sidecar?.totalPages,
            ),
            metadata,
        )
    }

    suspend fun readBook(
        config: ReadingSourceConfig,
        book: ResolvedBook,
        item: ReadingItem,
    ): EnrichedReadingItem {
        val metadata = metadata(config, book, item)
        return EnrichedReadingItem(
            item.copy(
                title = metadata.title,
                description = metadata.description,
                author = metadata.author,
                coverUri = metadata.coverUri,
            ),
            metadata,
        )
    }

    private suspend fun metadata(
        config: ReadingSourceConfig,
        book: ResolvedBook,
        item: ReadingItem,
    ): BookMetadata {
        val fingerprint = book.document.fingerprint()
        val old = previous(config, item.id.path)
        if (canReuse(old, fingerprint)) return checkNotNull(old)
        val key = covers.key(config, item.id.path, fingerprint ?: UUID.randomUUID().toString())
        var cover: String? = null
        val result = readCapability {
            books.metadata(book) { bytes -> cover = covers.write(key, bytes) }
        }.result
        checkMetadataSource(result)
        val parsed = (result as? ProbeRead.Success)?.value
        if (parsed == null && sameFingerprint(old, fingerprint)) return checkNotNull(old)
        return BookMetadata(
            fingerprint = fingerprint.takeIf { parsed != null },
            title = parsed?.title ?: item.title,
            author = parsed?.author,
            coverUri = cover,
            description = parsed?.description,
        )
    }

    private fun checkMetadataSource(result: ProbeRead<BookMetadataProbeResult>) {
        if (result !is ProbeRead.Failed) return
        if (result.issue in setOf(ProbeIssue.ProviderError, ProbeIssue.PermissionLost)) {
            throw java.io.IOException("Book metadata source unavailable: ${result.issue}")
        }
    }

    private fun sameFingerprint(old: BookMetadata?, fingerprint: String?): Boolean {
        if (fingerprint == null) return false
        return old?.fingerprint == fingerprint
    }

    private suspend fun canReuse(old: BookMetadata?, fingerprint: String?): Boolean {
        if (old == null) return false
        if (fingerprint == null) return false
        if (old.fingerprint != fingerprint) return false
        return covers.exists(old.coverUri)
    }
}

private fun readingStatus(status: String?): ReadingStatus = when (status) {
    "reading" -> ReadingStatus.Reading
    "complete" -> ReadingStatus.Finished
    "abandoned" -> ReadingStatus.Abandoned
    else -> ReadingStatus.Unknown
}
