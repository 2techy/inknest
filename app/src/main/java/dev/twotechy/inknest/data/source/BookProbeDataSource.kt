package dev.twotechy.inknest.data.source

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import dev.twotechy.inknest.data.source.epub.EpubProbe
import dev.twotechy.inknest.data.source.epub.EpubProbeResult
import dev.twotechy.inknest.data.source.epub.MAX_EPUB_BYTES
import dev.twotechy.inknest.data.source.epub.validateEpubSource
import dev.twotechy.inknest.data.source.lua.ReaderLuaDataSource
import dev.twotechy.inknest.data.source.lua.SidecarDto
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal data class BookChecksum(val tree: String, val fingerprint: String?, val checksum: String)

internal fun interface BookChecksumSource {
    suspend fun checksum(
        config: ReadingSourceConfig,
        path: String,
        cached: suspend (String, String) -> String?,
    ): BookChecksum
}

internal data class ResolvedBook(
    val sourcePath: String,
    val tree: String,
    val relative: List<String>,
    val document: SourceDocument,
)
internal class UnresolvedBookPath : IOException()

internal class BookProbeDataSource(
    private val files: SourceDocumentReader,
    private val cache: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BookChecksumSource {
    suspend fun resolve(config: ReadingSourceConfig, path: String): ResolvedBook =
        withContext(ioDispatcher) {
            val resolver = pathResolver()
            var failure: Exception? = null
            for (tree in config.booksTreeUris.sorted() + listOfNotNull(config.koreaderTreeUri)) {
                val uri = Uri.parse(tree)
                if (uri.authority != "com.android.externalstorage.documents") continue
                val relative =
                    resolver.relative(path, DocumentsContract.getTreeDocumentId(uri)) ?: continue
                try {
                    val document = files.describe(tree, relative)
                    if (document.mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        throw UnresolvedBookPath()
                    }
                    return@withContext ResolvedBook(path, tree, relative, document)
                } catch (error: SecurityException) {
                    failure = error
                } catch (error: IOException) {
                    failure = error
                }
            }
            failure?.let { throw it }
            throw UnresolvedBookPath()
        }

    suspend fun resolveIdentity(config: ReadingSourceConfig, path: String): ResolvedBook = resolve(
        config,
        if (path.startsWith("primary:")) {
            Environment.getExternalStorageDirectory().path + "/" + path.removePrefix("primary:")
        } else {
            path
        },
    )

    suspend fun canonicalPath(path: String): String = withContext(ioDispatcher) {
        pathResolver().relative(path, "primary:")?.let { "primary:" + it.joinToString("/") } ?: path
    }

    private fun pathResolver(): BookPathResolver {
        val canonical = Environment.getExternalStorageDirectory().canonicalFile
        val aliases = listOf(canonical.path, "/sdcard", "/storage/self/primary").filter {
            File(it).canonicalFile == canonical
        }.toSet()
        return BookPathResolver(aliases)
    }

    override suspend fun checksum(
        config: ReadingSourceConfig,
        path: String,
        cached: suspend (String, String) -> String?,
    ): BookChecksum = withContext(ioDispatcher) {
        val book = resolveIdentity(config, path)
        val fingerprint = book.document.fingerprint()
        // Even cache hits recheck the persisted grant and containment through resolveIdentity.
        val known = fingerprint?.let { cached(book.tree, it) }
        val checksum = known ?: readChecksum(book)
        BookChecksum(book.tree, fingerprint, checksum)
    }

    private suspend fun readChecksum(book: ResolvedBook): String {
        val direct = files as? DirectSourceDocumentReader
            ?: throw IOException("Provider does not support direct reads")
        return direct.readDocument(book.document) { input ->
            val channel =
                (input as? FileInputStream)?.channel
                    ?: throw IOException("Provider is not seekable")
            partialBookMd5(channel)
        }
    }

    suspend fun sidecar(book: ResolvedBook): SidecarDto {
        val name = book.relative.last()
        val ext = name.substringAfterLast('.', "")
        if (ext.isEmpty()) throw MissingSourceFile()
        val path =
            book.relative.dropLast(1) +
                listOf(name.substringBeforeLast('.') + ".sdr", "metadata.$ext.lua")
        return files.read(book.tree, path, ReaderLuaDataSource()::sidecar)
    }

    suspend fun epub(
        book: ResolvedBook,
        onCover: suspend (ByteArray) -> Unit = {},
    ): EpubProbeResult = withContext(ioDispatcher) {
        validateEpubSource(book.relative.last(), book.document.size)
        val dir = Files.createTempDirectory(cache.toPath(), "reader-epub-").toFile()
        try {
            val file = File(dir, "source.epub")
            val copy: suspend (java.io.InputStream) -> Unit = { input ->
                file.outputStream().use { copyBounded(input, it, MAX_EPUB_BYTES) }
            }
            if (files is DirectSourceDocumentReader) {
                files.readDocument(book.document, copy)
            } else {
                files.read(book.tree, book.relative, copy)
            }
            try {
                EpubProbe().read(file, onCover)
            } catch (
                error: SourceLimitExceeded,
            ) {
                throw error
            } catch (
                _: org.xmlpull.v1.XmlPullParserException,
            ) {
                throw InvalidBookMetadata()
            } catch (_: IOException) {
                throw InvalidBookMetadata()
            }
        } finally {
            check(dir.canonicalFile.parentFile == cache.canonicalFile)
            dir.deleteRecursively()
        }
    }
}
internal class UnsupportedBookFormat : IOException()

internal class InvalidBookMetadata : IOException()

// KOReader util.partialMD5: 1 KiB at 0, 1 KiB, 4 KiB, ... 1 GiB (at most 12 KiB read).
internal suspend fun partialBookMd5(channel: SeekableByteChannel): String {
    val digest = MessageDigest.getInstance("MD5")
    val buffer = ByteBuffer.allocate(1024)
    val offsets = listOf(0L) + (0..10).map { 1024L shl (2 * it) }
    for (offset in offsets) {
        currentCoroutineContext().ensureActive()
        channel.position(offset)
        readSample(channel, buffer)
        if (buffer.position() == 0) break
        digest.update(buffer.array(), 0, buffer.position())
    }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
}

private suspend fun readSample(channel: SeekableByteChannel, buffer: ByteBuffer) {
    buffer.clear()
    while (buffer.hasRemaining()) {
        currentCoroutineContext().ensureActive()
        val count = channel.read(buffer)
        if (count < 0) break
        if (count == 0) throw IOException("Provider returned no bytes")
    }
}
