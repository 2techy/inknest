package dev.twotechy.inknest.data.source.formats.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import dev.twotechy.inknest.data.source.BookMetadataProbeResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class PdfReader {
    suspend fun read(file: File, cover: suspend (ByteArray) -> Unit): BookMetadataProbeResult {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = try {
            PdfRenderer(descriptor)
        } catch (_: SecurityException) {
            descriptor.close()
            return BookMetadataProbeResult(null, null, false)
        } catch (error: Exception) {
            descriptor.close()
            throw error
        }
        renderer.use {
            if (it.pageCount == 0) return BookMetadataProbeResult(null, null, false)
            it.openPage(0).use { page ->
                val ratio = 480.0 / maxOf(page.width, page.height)
                val bitmap = Bitmap.createBitmap(
                    (page.width * ratio).toInt().coerceAtLeast(1),
                    (page.height * ratio).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
                try {
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    currentCoroutineContext().ensureActive()
                    val output = ByteArrayOutputStream()
                    if (!bitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            output,
                        )
                    ) {
                        throw IOException("Cover encoding failed")
                    }
                    cover(output.toByteArray())
                } finally {
                    bitmap.recycle()
                }
            }
        }
        return BookMetadataProbeResult(null, null, true)
    }
}
