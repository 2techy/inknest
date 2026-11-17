package dev.twotechy.inknest.data.source.formats

import dev.twotechy.inknest.data.source.formats.BookFileExtensions

internal fun bookFormatSource(name: String, size: Long?): Pair<String, Long> {
    val extension = BookFileExtensions.fromName(name)
    if (extension !in BookFileExtensions.metadata) {
        throw dev.twotechy.inknest.data.source.formats.UnsupportedBookFormat()
    }
    val limit = if (extension == BookFileExtensions.FB2) 64L * 1024 * 1024 else 256L * 1024 * 1024
    if ((size ?: 0) > limit) throw dev.twotechy.inknest.data.source.io.SourceLimitExceeded()
    return extension to limit
}
