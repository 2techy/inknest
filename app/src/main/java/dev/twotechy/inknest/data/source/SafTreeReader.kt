package dev.twotechy.inknest.data.source

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal class SafTreeReader(private val resolver: ContentResolver) : SourceFileReader {
    override suspend fun <T> read(
        tree: String,
        path: List<String>,
        parse: suspend (InputStream) -> T,
    ): T = withContext(Dispatchers.IO) {
        require(
            path.isNotEmpty() &&
                path.all { it.isNotBlank() && it != "." && it != ".." && '/' !in it },
        )
        val uri = Uri.parse(tree)
        require(uri.authority == "com.android.externalstorage.documents")
        require(PrimaryTreePath.parse(DocumentsContract.getTreeDocumentId(uri)) != null)
        var id = DocumentsContract.getTreeDocumentId(uri)
        for (name in path) {
            currentCoroutineContext().ensureActive()
            id = child(uri, id, name)
        }
        val document = DocumentsContract.buildDocumentUriUsingTree(uri, id)
        val input =
            resolver.openInputStream(document)
                ?: throw IOException("Provider returned no stream")
        input.use { parse(it) }
    }

    private fun child(tree: Uri, parent: String, name: String): String {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
        val columns =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            )
        val cursor =
            resolver.query(uri, columns, null, null, null)
                ?: throw IOException("Provider returned no listing")
        cursor.use {
            while (it.moveToNext()) if (it.getString(1) == name) return it.getString(0)
        }
        throw MissingSourceFile()
    }
}
