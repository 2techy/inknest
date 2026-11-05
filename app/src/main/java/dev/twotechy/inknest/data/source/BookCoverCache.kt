package dev.twotechy.inknest.data.source

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dev.twotechy.inknest.data.local.reading.readingCacheKey
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// Private, immutable thumbnails: source identity and file fingerprint determine the filename.
internal class BookCoverCache(private val directory: File) {
    private val writes = Mutex()
    private val generation = AtomicLong()
    fun key(config: ReadingSourceConfig, path: String, fingerprint: String?): String {
        val input = "${config.readingCacheKey()}\n$path\n$fingerprint"
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
    }

    suspend fun exists(uri: String?): Boolean = withContext(Dispatchers.IO) {
        uri == null || File(checkNotNull(Uri.parse(uri).path)).isFile
    }

    suspend fun write(key: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val epoch = generation.get()
        if (!directory.isDirectory &&
            !directory.mkdirs()
        ) {
            throw IOException("Cannot create cover cache")
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > 960) sample *= 2
        options.inJustDecodeBounds = false
        options.inSampleSize = sample
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IOException("Invalid cover")
        val ratio = minOf(1f, 480f / maxOf(decoded.width, decoded.height))
        val thumbnail = Bitmap.createScaledBitmap(
            decoded,
            maxOf(1, (decoded.width * ratio).toInt()),
            maxOf(1, (decoded.height * ratio).toInt()),
            true,
        )
        val temporary = File.createTempFile("cover-", ".tmp", directory)
        try {
            temporary.outputStream().use {
                if (!thumbnail.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        it,
                    )
                ) {
                    throw IOException("Cannot encode cover")
                }
            }
            val target = File(directory, "$key.png")
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
            Uri.fromFile(target).toString()
        } finally {
            temporary.delete()
            if (thumbnail !== decoded) thumbnail.recycle()
            decoded.recycle()
        }
    }

    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        directory.listFiles()?.sumOf { it.length() } ?: 0L
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        writes.withLock {
            generation.incrementAndGet()
            directory.listFiles()?.forEach { it.delete() }
        }
    }

    suspend fun prune(uris: Set<String>) = withContext(Dispatchers.IO) {
        val names = uris.mapNotNull { Uri.parse(it).lastPathSegment }.toSet()
        // A concurrent candidate may have just produced an uncommitted thumbnail.
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        writes.withLock {
            directory.listFiles()?.filter { it.name !in names && it.lastModified() < cutoff }
                ?.forEach { it.delete() }
        }
    }
}
