package dev.twotechy.inknest.platform

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import dev.twotechy.inknest.data.source.PrimaryTreePath

// Some devices expose SAF descriptors as /mnt/user/... paths that KOReader's Lua runtime
// cannot reopen. Its own FileProvider resolves the same original file in the reader's namespace.
internal fun koreaderDocumentIntent(original: Intent, providerOwner: (String) -> String?): Intent {
    if (original.`package` != KOREADER_PACKAGE) return original
    val source = original.data ?: return original
    val path = externalDocumentPath(source) ?: return original
    val authority = "$KOREADER_PACKAGE.provider"
    if (providerOwner(authority) != KOREADER_PACKAGE) return original
    val uri = Uri.Builder().scheme("content").authority(authority).appendPath("external").apply {
        path.segments.forEach { appendPath(it) }
    }.build()
    return Intent(original).apply {
        setDataAndType(uri, original.type)
        clipData = ClipData.newRawUri("Book", uri)
        // The receiving app owns this provider; InkNest must not grant another app's URI.
        flags = flags and URI_GRANTS.inv()
    }
}

private fun externalDocumentPath(uri: Uri): PrimaryTreePath? {
    if (uri.authority != "com.android.externalstorage.documents") return null
    val id = try {
        DocumentsContract.getDocumentId(uri)
    } catch (_: IllegalArgumentException) {
        return null
    }
    return PrimaryTreePath.parse(id)?.takeIf { it.segments.isNotEmpty() }
}

private const val KOREADER_PACKAGE = "org.koreader.launcher"
private const val URI_GRANTS = Intent.FLAG_GRANT_READ_URI_PERMISSION or
    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
