package dev.twotechy.inknest.platform.covers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import java.io.File
import java.io.IOException

internal class AndroidBookCoverDecoder(private val directory: File) : BookCoverDecoder {
    override suspend fun load(request: BookCoverRequest): DecodedBookCover? = try {
        val uri = request.uri.toUri()
        if (uri.scheme != "file") {
            null
        } else {
            val file = uri.path?.let(::File)?.canonicalFile
            if (file?.parentFile == directory.canonicalFile) decode(file, request) else null
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun decode(file: File, request: BookCoverRequest): DecodedBookCover? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (minOf(bounds.outWidth, bounds.outHeight) <= 0) return null
        if (maxOf(bounds.outWidth, bounds.outHeight) > BookCoverSize.MAX_EDGE) return null
        val plan = request.size.decodePlan(BookCoverSize(bounds.outWidth, bounds.outHeight))
        val options = BitmapFactory.Options().apply {
            inSampleSize = plan.sample
            inPreferredConfig =
                if (request.color) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        }
        val decoded = BitmapFactory.decodeFile(file.path, options) ?: return null
        var output: Bitmap? = null
        try {
            output = decoded.scale(plan.output.width, plan.output.height, filter = true)
            return DecodedBookCover(output.asImageBitmap(), output.allocationByteCount)
        } finally {
            if (decoded !== output) decoded.recycle()
        }
    }
}
