package dev.twotechy.inknest.data.source.formats.xml

internal class XmlNode(val name: String, val attrs: Map<String, String>) {
    val children = mutableListOf<XmlNode>()
    val text = StringBuilder()

    fun descendants(name: String): List<XmlNode> = children.flatMap { child ->
        listOfNotNull(child.takeIf { it.name == name }) + child.descendants(name)
    }
}

// A new reader owns the entire parser/stack budget for each bounded XML entry.
