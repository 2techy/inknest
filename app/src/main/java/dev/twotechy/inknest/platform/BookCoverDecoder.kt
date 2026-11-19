package dev.twotechy.inknest.platform

internal fun interface BookCoverDecoder {
    suspend fun load(request: BookCoverRequest): DecodedBookCover?
}
