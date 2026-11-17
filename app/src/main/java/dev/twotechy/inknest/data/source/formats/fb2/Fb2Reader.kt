package dev.twotechy.inknest.data.source.formats.fb2

import android.util.Base64
import dev.twotechy.inknest.data.source.formats.BookMetadataProbeResult
import dev.twotechy.inknest.data.source.formats.InvalidBookMetadata
import dev.twotechy.inknest.data.source.formats.optionalCover
import dev.twotechy.inknest.data.source.formats.plainDescription
import dev.twotechy.inknest.data.source.io.SourceLimitExceeded
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

internal class Fb2Reader {
    suspend fun read(file: File, cover: suspend (ByteArray) -> Unit): BookMetadataProbeResult =
        file.inputStream().use { input ->
            val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
                setInput(input, null)
            }
            val state = Fb2Metadata()
            val events = Fb2Events(state)
            while (parser.nextToken() != XmlPullParser.END_DOCUMENT) {
                currentCoroutineContext().ensureActive()
                events.accept(parser)
            }
            val found = state.binary?.let { encoded -> decodeCover(encoded, cover) } ?: false
            BookMetadataProbeResult(
                state.title,
                state.author(),
                found,
                plainDescription(state.annotation.toString()),
            )
        }
    private suspend fun decodeCover(encoded: String, cover: suspend (ByteArray) -> Unit): Boolean {
        val bytes = try {
            Base64.decode(encoded, Base64.DEFAULT)
        } catch (
            _: IllegalArgumentException,
        ) {
            return false
        }
        if (bytes.size > 8 * 1024 * 1024) return false
        return optionalCover(bytes, cover)
    }
}

private class Fb2Events(private val state: Fb2Metadata) {
    private var nodes = 0
    fun accept(parser: XmlPullParser) {
        when (parser.eventType) {
            XmlPullParser.DOCDECL -> throw InvalidBookMetadata()
            XmlPullParser.ENTITY_REF -> entity(parser)
            XmlPullParser.START_TAG -> start(parser)
            XmlPullParser.TEXT, XmlPullParser.CDSECT -> state.text(parser)
            XmlPullParser.END_TAG -> state.end(parser)
        }
    }
    private fun entity(parser: XmlPullParser) {
        if (parser.name !in setOf("amp", "lt", "gt", "apos", "quot")) throw InvalidBookMetadata()
        state.text(parser)
    }
    private fun start(parser: XmlPullParser) {
        if (nodes == 0 && parser.name != "FictionBook") throw InvalidBookMetadata()
        if (++nodes > 1_000_000 || parser.depth > 64) throw SourceLimitExceeded()
        state.start(parser)
    }
}

private class Fb2Metadata {
    private val path = mutableListOf<String>()
    private val values = mutableMapOf<String, StringBuilder>()
    private var firstAuthor = false
    private var authorComplete = false
    private var coverId: String? = null
    private var collectingBinary = false
    private val artwork = StringBuilder()
    val annotation = StringBuilder()
    val title get() = values["book-title"]?.toString()?.trim()?.takeIf(String::isNotBlank)
    val binary get() = artwork.toString().takeIf(String::isNotBlank)

    fun start(parser: XmlPullParser) {
        path += parser.name
        if (path ==
            listOf("FictionBook", "description", "title-info", "author")
        ) {
            firstAuthor =
                !authorComplete
        }
        if (isCoverReference(parser)) {
            coverId = parser.getAttributeValue("http://www.w3.org/1999/xlink", "href")
                ?.takeIf { it.startsWith('#') }?.removePrefix("#")
        }
        if (path == listOf("FictionBook", "binary")) {
            collectingBinary = coverId != null && parser.getAttributeValue(null, "id") == coverId
        }
    }

    fun text(parser: XmlPullParser) {
        val text = parser.text
        if (collectingBinary) appendArtwork(text)
        if (!inTitleInfo()) return
        if ("annotation" in path) {
            val remaining = (32 * 1024 - annotation.length).coerceAtLeast(0)
            annotation.append(android.text.TextUtils.htmlEncode(text.take(remaining)))
        } else if (metadataField()) {
            val value = values.getOrPut(path.last()) { StringBuilder() }
            if (value.length.toLong() + text.length > 2 * 1024 * 1024) throw SourceLimitExceeded()
            value.append(text)
        }
    }

    private fun appendArtwork(text: String) {
        if (artwork.length.toLong() + text.length > 12 * 1024 * 1024) {
            collectingBinary = false
            artwork.clear()
        } else {
            artwork.append(text)
        }
    }

    fun end(parser: XmlPullParser) {
        val name = parser.name
        if (annotationParagraph(parser)) {
            annotation.append("<br><br>".take((32 * 1024 - annotation.length).coerceAtLeast(0)))
        }
        if (firstAuthor && name == "author") {
            authorComplete = true
            firstAuthor = false
        }
        if (name == "binary") collectingBinary = false
        path.removeAt(path.lastIndex)
    }

    fun author(): String? = listOf("first-name", "middle-name", "last-name")
        .mapNotNull { values[it]?.toString()?.trim()?.takeIf(String::isNotBlank) }
        .joinToString(" ").ifBlank {
            values["nickname"]?.toString().orEmpty().trim()
        }.takeIf(String::isNotBlank)

    private fun isCoverReference(parser: XmlPullParser): Boolean {
        if (coverId != null) return false
        if (!inTitleInfo()) return false
        return parser.name == "image" && "coverpage" in path
    }
    private fun metadataField(): Boolean {
        if (path.last() == "book-title") return true
        if (!firstAuthor) return false
        return path.last() in setOf("first-name", "middle-name", "last-name", "nickname")
    }
    private fun annotationParagraph(parser: XmlPullParser): Boolean {
        if (!inTitleInfo()) return false
        if ("annotation" !in path) return false
        return parser.name in setOf("p", "empty-line")
    }

    private fun inTitleInfo() = path.take(3) == listOf("FictionBook", "description", "title-info")
}
