package dev.twotechy.inknest.data.source.formats

import java.io.IOException

internal suspend fun optionalCover(bytes: ByteArray, cover: suspend (ByteArray) -> Unit): Boolean =
    try {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            cover(bytes)
            true
        } else {
            false
        }
    } catch (_: IOException) {
        false
    }
