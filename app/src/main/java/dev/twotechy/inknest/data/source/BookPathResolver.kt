package dev.twotechy.inknest.data.source

internal class BookPathResolver(private val primaryAliases: Set<String>) {
    fun relative(path: String, grantedDocumentId: String): List<String>? {
        val root = PrimaryTreePath.parse(grantedDocumentId) ?: return null
        val absolute = segments(path) ?: return null
        val alias = primaryAliases.mapNotNull(::segments).filter { absolute.take(it.size) == it }
            .maxByOrNull { it.size } ?: return null
        return PrimaryTreePath(absolute.drop(alias.size)).relativeTo(root)?.takeIf {
            it.isNotEmpty()
        }
    }

    private fun segments(path: String): List<String>? {
        if (!path.startsWith('/')) return null
        val parts = path.removePrefix("/").trimEnd('/').split('/')
        if (parts.any { it.isEmpty() || it == "." || it == ".." || '\u0000' in it }) return null
        return parts
    }
}
