package dev.twotechy.inknest.platform.covers

internal fun interface BookCoverDecoder {
    suspend fun load(request: BookCoverRequest): DecodedBookCover?
}
