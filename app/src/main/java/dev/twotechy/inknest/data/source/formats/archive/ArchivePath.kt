package dev.twotechy.inknest.data.source.formats.archive

import java.net.URI

internal fun archivePath(base: String, href: String): String? {
    val uri = localArchiveUri(href) ?: return null
    val path = uri.path ?: return null
    if (unsafeArchivePath(path)) return null
    val result = base.substringBeforeLast(
        '/',
        "",
    ).split('/').filter(String::isNotEmpty).toMutableList()
    for (part in path.split('/')) {
        if (!appendArchiveSegment(result, part)) return null
    }
    return result.joinToString("/").takeIf(String::isNotEmpty)
}

private fun localArchiveUri(href: String): URI? {
    val uri = try {
        URI(href)
    } catch (_: Exception) {
        return null
    }
    if (uri.isAbsolute) return null
    if (uri.rawAuthority != null) return null
    if (uri.rawQuery != null) return null
    if (uri.rawFragment != null) return null
    return uri
}

private fun unsafeArchivePath(path: String): Boolean =
    path.startsWith('/') || '\\' in path || '\u0000' in path

private fun appendArchiveSegment(parts: MutableList<String>, part: String): Boolean {
    when (part) {
        "", "." -> Unit

        ".." -> {
            if (parts.isEmpty()) return false
            parts.removeAt(parts.lastIndex)
        }

        else -> parts += part
    }
    return true
}
