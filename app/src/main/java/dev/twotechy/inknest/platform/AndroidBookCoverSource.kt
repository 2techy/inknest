package dev.twotechy.inknest.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import java.io.File
import java.io.IOException

internal class AndroidBookCoverSource(private val directory: File) : BookCoverSource {
    override suspend fun load(request: BookCoverRequest): DecodedBookCover? = try {
        val uri = request.uri.toUri()
        if (uri.scheme != "file") {
            null
        } else {
            val file = uri.path?.let(::File)?.canonicalFile
            if (file?.parentFile == directory.canonicalFile) decode(file, request.size) else null
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun decode(file: File, target: BookCoverSize): DecodedBookCover? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (minOf(bounds.outWidth, bounds.outHeight) <= 0) return null
        if (maxOf(bounds.outWidth, bounds.outHeight) > BookCoverSize.MAX_EDGE) return null
        val plan = target.decodePlan(BookCoverSize(bounds.outWidth, bounds.outHeight))
        val options = BitmapFactory.Options().apply {
            inSampleSize = plan.sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.path, options) ?: return null
        var output: Bitmap? = null
        try {
            output = Bitmap.createScaledBitmap(decoded, plan.output.width, plan.output.height, true)
            return DecodedBookCover(output.asImageBitmap(), output.allocationByteCount)
        } finally {
            if (decoded !== output) decoded.recycle()
        }
    }
}
