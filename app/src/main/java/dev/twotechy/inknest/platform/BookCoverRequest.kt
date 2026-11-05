package dev.twotechy.inknest.platform

internal data class BookCoverRequest(val uri: String, val size: BookCoverSize)

internal data class BookCoverSize(val width: Int, val height: Int) {
    fun decodePlan(source: BookCoverSize): BookCoverDecodePlan {
        val scale = minOf(width.toFloat() / source.width, height.toFloat() / source.height, 1f)
        val output = BookCoverSize(
            maxOf(1, (source.width * scale).toInt()),
            maxOf(1, (source.height * scale).toInt()),
        )
        var sample = 1
        while (minOf(
                source.width / (sample * 2) / output.width,
                source.height / (sample * 2) / output.height,
            ) >= 1
        ) {
            sample *= 2
        }
        return BookCoverDecodePlan(output, sample)
    }

    companion object {
        const val MAX_EDGE = 480

        // Nearby layout widths share a cache key without decoding a full thumbnail every time.
        fun forBounds(width: Int, height: Int): BookCoverSize {
            val bucketed = listOf(width, height).map {
                minOf(MAX_EDGE, ((it.coerceIn(1, MAX_EDGE) + 63) / 64) * 64)
            }
            return BookCoverSize(bucketed[0], bucketed[1])
        }
    }
}

internal data class BookCoverDecodePlan(val output: BookCoverSize, val sample: Int)
