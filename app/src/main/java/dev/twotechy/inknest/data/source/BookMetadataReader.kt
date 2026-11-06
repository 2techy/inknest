package dev.twotechy.inknest.data.source

import dev.twotechy.inknest.data.source.ProbeRead
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
                author = metadata.author,
                coverUri = metadata.coverUri,
                progress = sidecar?.percent?.let { (it * 100).roundToInt() },
                status = readingStatus(sidecar?.status),
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
        val epub = (
            readCapability {
                books.epub(book) { bytes -> cover = covers.write(key, bytes) }
            }.result as? ProbeRead.Success
            )?.value
        return BookMetadata(
            fingerprint = fingerprint.takeIf { epub != null },
            title = epub?.title ?: item.title,
            author = epub?.author,
            coverUri = cover,
        )
    }

    private suspend fun canReuse(old: BookMetadata?, fingerprint: String?): Boolean {
        if (old == null) return false
        if (fingerprint == null) return false
        if (old.fingerprint != fingerprint) return false
        return covers.exists(old.coverUri)
    }
}

private fun SourceDocument.fingerprint(): String? {
    val size = size ?: return null
    val modified = modified ?: return null
    if (modified <= 0) return null
    return "$size:$modified"
}

private fun readingStatus(status: String?): ReadingStatus = when (status) {
    "reading" -> ReadingStatus.Reading
    "complete" -> ReadingStatus.Finished
    "abandoned" -> ReadingStatus.Abandoned
    else -> ReadingStatus.Unknown
}
