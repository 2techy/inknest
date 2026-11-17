package dev.twotechy.inknest.data.source.impl

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import dev.twotechy.inknest.data.source.BookChecksum
import dev.twotechy.inknest.data.source.BookChecksumSource
import dev.twotechy.inknest.data.source.BookMetadataProbeResult
import dev.twotechy.inknest.data.source.BookPathResolver
import dev.twotechy.inknest.data.source.DirectSourceDocumentReader
import dev.twotechy.inknest.data.source.InvalidBookMetadata
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.ResolvedBook
import dev.twotechy.inknest.data.source.SourceDocumentReader
import dev.twotechy.inknest.data.source.SourceLimitExceeded
import dev.twotechy.inknest.data.source.UnresolvedBookPath
import dev.twotechy.inknest.data.source.copyBounded
import dev.twotechy.inknest.data.source.fingerprint
import dev.twotechy.inknest.data.source.formats.BookReader
import dev.twotechy.inknest.data.source.formats.bookFormatSource
import dev.twotechy.inknest.data.source.lua.ReaderLuaDataSource
import dev.twotechy.inknest.data.source.lua.SidecarDto
import dev.twotechy.inknest.data.source.partialBookMd5
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun metadata(
        book: ResolvedBook,
        onCover: suspend (ByteArray) -> Unit = {},
    ): BookMetadataProbeResult = withContext(ioDispatcher) {
        val (extension, limit) = bookFormatSource(book.relative.last(), book.document.size)
        val dir = Files.createTempDirectory(cache.toPath(), "reader-book-").toFile()
        try {
            val file = File(dir, "source.$extension")
            val copy: suspend (java.io.InputStream) -> Unit = { input ->
                file.outputStream().use { copyBounded(input, it, limit) }
            }
            if (files is DirectSourceDocumentReader) {
                files.readDocument(book.document, copy)
            } else {
                files.read(book.tree, book.relative, copy)
            }
            try {
                BookReader().read(file, extension, onCover)
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
