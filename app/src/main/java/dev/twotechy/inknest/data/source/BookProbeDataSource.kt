package dev.twotechy.inknest.data.source

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import dev.twotechy.inknest.data.source.epub.EpubProbe
import dev.twotechy.inknest.data.source.epub.EpubProbeResult
import dev.twotechy.inknest.data.source.lua.ReaderLuaDataSource
import dev.twotechy.inknest.data.source.lua.SidecarDto
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
) {
    suspend fun resolve(config: ReadingSourceConfig, path: String): ResolvedBook =
        withContext(Dispatchers.IO) {
            val resolver = pathResolver()
            for (tree in listOfNotNull(config.booksTreeUri, config.koreaderTreeUri).distinct()) {
                val uri = Uri.parse(tree)
                if (uri.authority != "com.android.externalstorage.documents") continue
                val relative =
                    resolver.relative(path, DocumentsContract.getTreeDocumentId(uri)) ?: continue
                val document = files.describe(tree, relative)
                if (document.mime ==
                    DocumentsContract.Document.MIME_TYPE_DIR
                ) {
                    throw UnresolvedBookPath()
                }
                return@withContext ResolvedBook(path, tree, relative, document)
            }
            throw UnresolvedBookPath()
        }

    suspend fun canonicalPath(path: String): String = withContext(Dispatchers.IO) {
        pathResolver().relative(path, "primary:")?.let { "primary:" + it.joinToString("/") } ?: path
    }

    private fun pathResolver(): BookPathResolver {
        val canonical = Environment.getExternalStorageDirectory().canonicalFile
        val aliases = listOf(canonical.path, "/sdcard", "/storage/self/primary").filter {
            File(it).canonicalFile == canonical
        }.toSet()
        return BookPathResolver(aliases)
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

    suspend fun epub(book: ResolvedBook): EpubProbeResult = withContext(Dispatchers.IO) {
        if (!book.relative.last().endsWith(
                ".epub",
                ignoreCase = true,
            )
        ) {
            throw UnsupportedBookFormat()
        }
        if (book.document.size != null &&
            book.document.size > 256L * 1024 * 1024
        ) {
            throw SourceLimitExceeded()
        }
        val dir = Files.createTempDirectory(cache.toPath(), "reader-epub-").toFile()
        try {
            val file = File(dir, "source.epub")
            files.read(book.tree, book.relative) { input ->
                file.outputStream().use { copyBounded(input, it, 256L * 1024 * 1024) }
            }
            try {
                EpubProbe().read(file)
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
