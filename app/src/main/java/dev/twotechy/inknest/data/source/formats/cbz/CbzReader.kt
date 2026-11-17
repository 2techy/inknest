package dev.twotechy.inknest.data.source.formats.cbz

import dev.twotechy.inknest.data.source.BookMetadataProbeResult
import dev.twotechy.inknest.data.source.SourceLimitExceeded
import dev.twotechy.inknest.data.source.copyBounded
import dev.twotechy.inknest.data.source.formats.BookFileExtensions
import dev.twotechy.inknest.data.source.formats.optionalCover
import dev.twotechy.inknest.data.source.formats.plainDescription
import dev.twotechy.inknest.data.source.formats.xml.XmlNode
import dev.twotechy.inknest.data.source.formats.xml.readBoundedXml
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.xmlpull.v1.XmlPullParserException

internal class CbzReader {
    suspend fun read(file: File, cover: suspend (ByteArray) -> Unit): BookMetadataProbeResult =
        ZipFile(file).use { zip ->
            if (zip.size() > 100_000) throw SourceLimitExceeded()
            val images = mutableListOf<ZipEntry>()
            for (entry in zip.entries()) {
                currentCoroutineContext().ensureActive()
                if (safeImage(entry)) images += entry
            }
            images.sortWith { a, b -> naturalCompare(a.name, b.name) }
            val metadata = metadata(zip)
            val index = metadata?.descendants("Page")?.firstOrNull {
                "FrontCover" in it.attrs["Type"].orEmpty().split(' ')
            }?.attrs?.get("Image")?.toIntOrNull()
            val selected = index?.let(images::getOrNull) ?: images.firstOrNull()
            val found = if (selected == null) {
                false
            } else {
                try {
                    optionalCover(bytes(zip, selected, 8L * 1024 * 1024), cover)
                } catch (_: IOException) {
                    false
                }
            }
            BookMetadataProbeResult(
                metadata?.value("Title"),
                metadata?.value("Writer"),
                found,
                plainDescription(metadata?.value("Summary")),
            )
        }

    private suspend fun metadata(zip: ZipFile): XmlNode? = try {
        zip.getEntry("ComicInfo.xml")?.let { readBoundedXml(bytes(zip, it, 2L * 1024 * 1024)) }
    } catch (_: IOException) {
        null
    } catch (_: XmlPullParserException) {
        null
    }

    private suspend fun bytes(zip: ZipFile, entry: ZipEntry, limit: Long): ByteArray {
        if (entry.isDirectory || entry.size > limit) throw SourceLimitExceeded()
        return zip.getInputStream(entry).use { input ->
            val output = ByteArrayOutputStream()
            copyBounded(input, output, limit)
            output.toByteArray()
        }
    }

    private fun safeImage(entry: ZipEntry): Boolean {
        val parts = entry.name.split('/')
        return !entry.isDirectory && !entry.name.startsWith('/') && '\\' !in entry.name &&
            parts.none { it.startsWith('.') || it == "__MACOSX" || ':' in it } &&
            BookFileExtensions.fromName(entry.name) in BookFileExtensions.coverImages
    }
}

private fun XmlNode.value(name: String) =
    descendants(name).firstOrNull()?.text?.toString()?.trim()?.takeIf(String::isNotBlank)

internal fun naturalCompare(left: String, right: String): Int {
    val a = NATURAL_PARTS.findAll(left.lowercase()).map { it.value }.toList()
    val b = NATURAL_PARTS.findAll(right.lowercase()).map { it.value }.toList()
    for (i in 0 until minOf(a.size, b.size)) {
        val x = a[i]
        val y = b[i]
        val order = if (x.first().isDigit() && y.first().isDigit()) {
            val nx = x.trimStart('0').ifEmpty { "0" }
            val ny = y.trimStart('0').ifEmpty { "0" }
            nx.length.compareTo(ny.length).takeIf { it != 0 } ?: nx.compareTo(ny)
        } else {
            x.compareTo(y)
        }
        if (order != 0) return order
    }
    return a.size.compareTo(b.size).takeIf { it != 0 } ?: left.compareTo(right)
}

private val NATURAL_PARTS = Regex("[0-9]+|[^0-9]+")
