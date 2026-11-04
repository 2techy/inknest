package dev.twotechy.inknest.data.source.epub

import dev.twotechy.inknest.data.source.SourceLimitExceeded
import java.io.IOException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

internal fun readBoundedXml(bytes: ByteArray): XmlNode = BoundedXmlReader(bytes).read()

internal class XmlNode(val name: String, val attrs: Map<String, String>) {
    val children = mutableListOf<XmlNode>()
    val text = StringBuilder()

    fun descendants(name: String): List<XmlNode> = children.flatMap { child ->
        listOfNotNull(child.takeIf { it.name == name }) + child.descendants(name)
    }
}

// A new reader owns the entire parser/stack budget for each bounded XML entry.
internal class BoundedXmlReader(bytes: ByteArray) {
    private val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        setInput(bytes.inputStream(), null)
    }
    private val root = XmlNode("root", emptyMap())
    private val stack = mutableListOf(root)
    private var count = 0

    fun read(): XmlNode {
        while (true) {
            when (parser.nextToken()) {
                XmlPullParser.END_DOCUMENT -> return root
                XmlPullParser.DOCDECL -> throw IOException("DTD not supported")
                XmlPullParser.START_TAG -> startNode()
                XmlPullParser.END_TAG -> endNode()
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> stack.last().text.append(parser.text)
                XmlPullParser.ENTITY_REF -> appendEntity()
            }
        }
    }

    private fun startNode() {
        if (++count > 50_000 || stack.size >= 64) throw SourceLimitExceeded()
        val attrs = (0 until parser.attributeCount).associate {
            parser.getAttributeName(it) to parser.getAttributeValue(it)
        }
        val node = XmlNode(parser.name, attrs)
        stack.last().children += node
        stack += node
    }

    private fun endNode() {
        if (stack.size <= 1) throw IOException("Invalid XML")
        stack.removeAt(stack.lastIndex)
    }

    private fun appendEntity() {
        if (parser.name !in setOf("amp", "lt", "gt", "apos", "quot")) {
            throw IOException("Entity not supported")
        }
        stack.last().text.append(parser.text)
    }
}
