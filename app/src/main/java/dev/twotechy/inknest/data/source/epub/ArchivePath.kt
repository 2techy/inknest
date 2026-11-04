package dev.twotechy.inknest.data.source.epub

import java.net.URI

internal fun archivePath(base: String, href: String): String? {
    val uri = try {
        URI(href)
    } catch (_: Exception) {
        return null
    }
    if (uri.isAbsolute || uri.rawAuthority != null || uri.rawQuery != null ||
        uri.rawFragment != null
    ) {
        return null
    }
    val path = uri.path ?: return null
    if (path.startsWith('/') || '\\' in path || '\u0000' in path) return null
    val result = base.substringBeforeLast('/', "").split('/').filter {
        it.isNotEmpty()
    }.toMutableList()
    for (part in path.split('/')) {
        when (part) {
            "", "." -> Unit
            ".." -> if (result.isEmpty()) return null else result.removeAt(result.lastIndex)
            else -> result += part
        }
    }
    return result.joinToString("/").takeIf { it.isNotEmpty() }
}
