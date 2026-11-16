package dev.twotechy.inknest.data.source.epub

import android.graphics.BitmapFactory
import android.text.Html
import dev.twotechy.inknest.data.source.SourceLimitExceeded
import dev.twotechy.inknest.data.source.UnsupportedBookFormat
import dev.twotechy.inknest.data.source.copyBounded
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.zip.ZipFile

internal data class EpubProbeResult(
    val title: String?,
    val author: String?,
    val cover: Boolean,
    val description: String? = null,
)

internal class EpubProbe {
    suspend fun read(file: File, onCover: suspend (ByteArray) -> Unit = {}): EpubProbeResult =
        ZipFile(file).use { zip ->
            if (zip.size() > 100_000) throw SourceLimitExceeded()
            val container = readBoundedXml(entry(zip, "META-INF/container.xml", XML_LIMIT))
            val opfName = container.descendants("rootfile").firstOrNull()?.attrs?.get("full-path")
                ?.let { archivePath("", it) } ?: throw IOException("Missing package document")
            val opf = readBoundedXml(entry(zip, opfName, XML_LIMIT))
            val metadata =
                opf.descendants("metadata").firstOrNull() ?: throw IOException("Missing metadata")
            val title = metadata.descendants("title").firstOrNull()?.text?.toString()
                ?.trim()?.takeIf(String::isNotEmpty)
            val author = metadata.descendants(
                "creator",
            ).firstOrNull()?.text?.toString()?.trim()?.takeIf(String::isNotEmpty)
            val manifest = opf.descendants("manifest").firstOrNull()?.children.orEmpty()
            val coverId = metadata.descendants("meta").firstOrNull {
                it.attrs["name"] == "cover"
            }?.attrs?.get("content")
            val coverItem = manifest.firstOrNull {
                it.attrs["properties"].orEmpty().split(' ').contains("cover-image")
            } ?: manifest.firstOrNull { coverId != null && it.attrs["id"] == coverId }
            val cover = coverItem?.attrs?.get("href")?.let { href ->
                val path = archivePath(opfName, href) ?: throw IOException("Invalid cover path")
                readCover(zip, path, onCover)
            } ?: false
            val description = metadata.descendants(
                "description",
            ).firstOrNull()?.let(::epubDescription)
            EpubProbeResult(title, author, cover, description)
        }

    private suspend fun readCover(
        zip: ZipFile,
        path: String,
        onCover: suspend (ByteArray) -> Unit,
    ): Boolean = try {
        val bytes = entry(zip, path, COVER_LIMIT)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth > 0 && options.outHeight > 0) {
            onCover(bytes)
            true
        } else {
            false
        }
    } catch (_: IOException) {
        // Missing, unsupported or damaged artwork must not discard valid book metadata.
        false
    }

    private suspend fun entry(zip: ZipFile, path: String, limit: Long): ByteArray {
        val item = zip.getEntry(path) ?: throw IOException("Missing archive entry")
        if (item.isDirectory) throw SourceLimitExceeded()
        if (maxOf(item.size, item.compressedSize) > limit) throw SourceLimitExceeded()
        return zip.getInputStream(item).use { input ->
            val output = ByteArrayOutputStream()
            copyBounded(input, output, limit)
            output.toByteArray()
        }
    }

    private companion object {
        const val XML_LIMIT = 2L * 1024 * 1024
        const val COVER_LIMIT = 8L * 1024 * 1024
    }
}

// Render only text; never load EPUB markup, links or resources into a WebView.
internal fun epubDescription(node: XmlNode): String? {
    val markup = node.text.toString().replace("\r\n", "\n").replace("\n", "<br>")
    val text = Html.fromHtml(markup, Html.FROM_HTML_MODE_LEGACY).toString()
        .replace('\u00a0', ' ')
        .lineSequence().map(String::trim).joinToString("\n")
        .replace(Regex("\n{3,}"), "\n\n").trim()
    val bytes = text.toByteArray(Charsets.UTF_8)
    val bounded = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.IGNORE)
        .decode(ByteBuffer.wrap(bytes, 0, bytes.size.coerceAtMost(32 * 1024))).toString()
    return bounded.takeIf(String::isNotBlank)
}

internal const val MAX_EPUB_BYTES = 256L * 1024 * 1024

internal fun validateEpubSource(name: String, size: Long?) {
    if (!name.endsWith(".epub", ignoreCase = true)) throw UnsupportedBookFormat()
    if (size != null && size > MAX_EPUB_BYTES) throw SourceLimitExceeded()
}
