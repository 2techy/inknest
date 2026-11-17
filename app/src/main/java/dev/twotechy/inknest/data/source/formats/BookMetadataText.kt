package dev.twotechy.inknest.data.source.formats

import android.text.Html

internal fun plainDescription(value: String?): String? {
    if (value == null) return null
    val text = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    val bytes = text.toByteArray(Charsets.UTF_8)
    return Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.IGNORE)
        .decode(java.nio.ByteBuffer.wrap(bytes, 0, minOf(bytes.size, 32 * 1024)))
        .toString().takeIf(String::isNotBlank)
}
