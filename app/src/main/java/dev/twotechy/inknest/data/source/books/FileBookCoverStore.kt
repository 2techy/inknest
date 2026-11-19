package dev.twotechy.inknest.data.source.books

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import androidx.core.net.toUri
import dev.twotechy.inknest.data.local.reading.readingCacheKey
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// Private, immutable thumbnails: source identity and file fingerprint determine the filename.
internal class FileBookCoverStore(
    private val directory: File,
    private val onClear: () -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val writes = Mutex()
    private val generation = AtomicLong()
    fun key(config: ReadingSourceConfig, path: String, fingerprint: String?): String {
        val input = "$THUMBNAIL_FORMAT_VERSION\n${config.readingCacheKey()}\n$path\n$fingerprint"
        val digest = MessageDigest.getInstance(
            "SHA-256",
        ).digest(input.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
        return "v$THUMBNAIL_FORMAT_VERSION-$digest"
    }

    suspend fun exists(uri: String?): Boolean = withContext(ioDispatcher) {
        isReusable(uri?.toUri())
    }

    private fun isReusable(uri: Uri?): Boolean {
        if (uri == null) return true
        if (uri.scheme != "file") return false
        val file = uri.path?.let(::File)?.canonicalFile ?: return false
        return file.parentFile == directory.canonicalFile &&
            file.name.matches(CURRENT_THUMBNAIL_NAME) && file.isFile
    }

    suspend fun write(key: String, bytes: ByteArray): String = withContext(ioDispatcher) {
        writeThumbnail(File(directory, "$key.webp"), bytes)
    }

    private suspend fun writeThumbnail(target: File, bytes: ByteArray): String {
        val epoch = generation.get()
        if (!directory.isDirectory &&
            !directory.mkdirs()
        ) {
            throw IOException("Cannot create cover cache")
        }
        val thumbnail = thumbnail(bytes)
        try {
            return encodeAndCommit(target, thumbnail, epoch)
        } finally {
            thumbnail.recycle()
        }
    }

    private fun thumbnail(bytes: ByteArray): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > 960) sample *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sample
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IOException("Invalid cover")
        try {
            val ratio = minOf(1f, 480f / maxOf(decoded.width, decoded.height))
            val result = decoded.scale(
                maxOf(1, (decoded.width * ratio).toInt()),
                maxOf(1, (decoded.height * ratio).toInt()),
                filter = true,
            )
            if (result !== decoded) decoded.recycle()
            return result
        } catch (error: Exception) {
            decoded.recycle()
            throw error
        }
    }

    private suspend fun encodeAndCommit(target: File, thumbnail: Bitmap, epoch: Long): String {
        val temporary = File.createTempFile("cover-", ".tmp", directory)
        try {
            temporary.outputStream().use {
                if (!thumbnail.compress(
                        compressionFormat(),
                        THUMBNAIL_QUALITY,
                        it,
                    )
                ) {
                    throw IOException("Cannot encode cover")
                }
            }
            currentCoroutineContext().ensureActive()
            writes.withLock {
                if (epoch != generation.get()) throw IOException("Cover cache cleared")
                Files.move(
                    temporary.toPath(),
                    target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
            return Uri.fromFile(target).toString()
        } finally {
            temporary.delete()
        }
    }

    // WEBP is the encoder available on API 26–29; newer APIs expose the explicit lossy format.
    @Suppress("DEPRECATION")
    private fun compressionFormat() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        Bitmap.CompressFormat.WEBP
    }

    suspend fun sizeBytes(): Long = withContext(ioDispatcher) {
        directory.listFiles()?.sumOf { it.length() } ?: 0L
    }

    suspend fun clear() = withContext(ioDispatcher) {
        writes.withLock {
            generation.incrementAndGet()
            directory.listFiles()?.forEach { it.delete() }
            onClear()
        }
    }

    suspend fun prune(uris: Set<String>) = withContext(ioDispatcher) {
        val names = uris.mapNotNull { it.toUri().lastPathSegment }.toSet()
        // A concurrent candidate may have just produced an uncommitted thumbnail.
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        writes.withLock {
            directory.listFiles()?.filter { it.name !in names && it.lastModified() < cutoff }
                ?.forEach { it.delete() }
        }
    }
}

private const val THUMBNAIL_FORMAT_VERSION = 1
private const val THUMBNAIL_QUALITY = 85
private val CURRENT_THUMBNAIL_NAME = Regex("v$THUMBNAIL_FORMAT_VERSION-[0-9a-f]{64}\\.webp")
