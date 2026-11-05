package dev.twotechy.inknest.data.source

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SafTreeReader(private val resolver: ContentResolver) : DirectSourceDocumentReader {
    override suspend fun <T> read(
        tree: String,
        path: List<String>,
        parse: suspend (InputStream) -> T,
    ): T = withContext(Dispatchers.IO) {
        val document = Uri.parse(describe(tree, path).uri)
        readUri(document, parse)
    }

    override suspend fun <T> readDocument(
        document: SourceDocument,
        parse: suspend (InputStream) -> T,
    ): T = withContext(Dispatchers.IO) {
        val uri = Uri.parse(document.uri)
        val tree = DocumentsContract.buildTreeDocumentUri(
            uri.authority,
            DocumentsContract.getTreeDocumentId(uri),
        )
        checkedTreeUri(tree.toString())
        val root = requireNotNull(PrimaryTreePath.parse(DocumentsContract.getTreeDocumentId(uri)))
        val child = PrimaryTreePath.parse(DocumentsContract.getDocumentId(uri))
        if (child?.relativeTo(root) == null) throw IOException("Outside tree")
        readUri(uri, parse)
    }

    private suspend fun <T> readUri(document: Uri, parse: suspend (InputStream) -> T): T {
        val input =
            safProviderOperation { resolver.openInputStream(document) }
                ?: throw IOException("Provider returned no stream")
        return input.use {
            coroutineScope {
                val closer = launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        input.close()
                    }
                }
                try {
                    parse(input)
                } finally {
                    closer.cancel()
                }
            }
        }
    }

    override suspend fun describe(tree: String, path: List<String>): SourceDocument =
        withContext(Dispatchers.IO) {
            describeInTree(tree, path)
        }

    private suspend fun describeInTree(tree: String, path: List<String>): SourceDocument {
        validatePath(path)
        val uri = checkedTreeUri(tree)
        val id = resolveDocumentId(uri, path)
        return readMetadata(DocumentsContract.buildDocumentUriUsingTree(uri, id))
    }

    private fun validatePath(path: List<String>) {
        require(
            path.all {
                it.isNotBlank() && it != "." && it != ".." && '/' !in it &&
                    '\u0000' !in it
            },
        )
    }

    private fun checkedTreeUri(tree: String): Uri {
        val uri = Uri.parse(tree)
        require(
            uri.authority == "com.android.externalstorage.documents" &&
                DocumentsContract.isTreeUri(uri),
        )
        requireNotNull(PrimaryTreePath.parse(DocumentsContract.getTreeDocumentId(uri)))
        if (resolver.persistedUriPermissions.none { it.uri == uri && it.isReadPermission }) {
            throw SecurityException("Persisted folder access was lost")
        }
        return uri
    }

    private suspend fun resolveDocumentId(tree: Uri, path: List<String>): String {
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val root = requireNotNull(PrimaryTreePath.parse(rootId))
        var id = rootId
        for (name in path) {
            currentCoroutineContext().ensureActive()
            id = child(tree, id, name)
            if (PrimaryTreePath.parse(id)?.relativeTo(root) ==
                null
            ) {
                throw IOException("Outside tree")
            }
        }
        return id
    }

    private suspend fun readMetadata(document: Uri): SourceDocument {
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        return cancellableQuery { signal ->
            val cursor = safProviderOperation {
                resolver.query(document, columns, null, null, null, signal)
            }
                ?: throw IOException("No metadata")
            cursor.use {
                if (!it.moveToFirst()) throw MissingSourceFile()
                val size = if (it.isNull(1)) null else it.getLong(1)
                val modified = if (it.isNull(2)) null else it.getLong(2)
                SourceDocument(document.toString(), it.getString(0), size, modified)
            }
        }
    }

    private suspend fun child(tree: Uri, parent: String, name: String): String {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
        val columns =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            )
        val context = currentCoroutineContext()
        return cancellableQuery { signal ->
            val cursor = safProviderOperation {
                resolver.query(uri, columns, null, null, null, signal)
            }
                ?: throw IOException("Provider returned no listing")
            cursor.use {
                var count = 0
                while (it.moveToNext()) {
                    context.ensureActive()
                    if (++count > 100_000) throw SourceLimitExceeded()
                    if (it.getString(1) == name) return@cancellableQuery it.getString(0)
                }
            }
            throw MissingSourceFile()
        }
    }
}

internal data class SourceDocument(
    val uri: String,
    val mime: String?,
    val size: Long?,
    val modified: Long?,
)
