package dev.twotechy.inknest.data.source.impl

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import dev.twotechy.inknest.data.source.BookCatalogSource
import dev.twotechy.inknest.data.source.CatalogBook
import dev.twotechy.inknest.data.source.PrimaryTreePath
import dev.twotechy.inknest.data.source.SourceDocument
import dev.twotechy.inknest.data.source.SourceLimitExceeded
import dev.twotechy.inknest.data.source.cancellableQuery
import dev.twotechy.inknest.data.source.formats.BookFileExtensions
import dev.twotechy.inknest.data.source.safProviderOperation
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

// Traverses only the granted primary Books tree. No symlinks, sidecars or hidden folders.
internal class SafBookCatalogSource(private val resolver: ContentResolver) : BookCatalogSource {
    override suspend fun scan(tree: String): List<CatalogBook> = withContext(Dispatchers.IO) {
        val uri = Uri.parse(tree)
        if (resolver.persistedUriPermissions.none { it.uri == uri && it.isReadPermission }) {
            throw SecurityException("Books folder permission lost")
        }
        if (uri.authority !=
            "com.android.externalstorage.documents"
        ) {
            throw IOException("Unsupported provider")
        }
        val rootId = DocumentsContract.getTreeDocumentId(uri)
        val root = PrimaryTreePath.parse(rootId) ?: throw IOException("Unsupported volume")
        val queue = ArrayDeque<String>().apply { add(rootId) }
        val seen = mutableSetOf<String>()
        val books = mutableListOf<CatalogBook>()
        var count = 0
        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val parent = queue.removeFirst()
            if (!seen.add(parent)) continue
            for ((id, name, document) in children(uri, parent)) {
                if (++count > 100_000) throw SourceLimitExceeded()
                val relative = PrimaryTreePath.parse(id)?.relativeTo(root)
                    ?: throw IOException("Document escaped Books folder")
                if (relative.size > 64) throw SourceLimitExceeded()
                if (name.startsWith('.') || name.endsWith(".sdr", true)) continue
                if (document.mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    queue.add(id)
                } else if (BookFileExtensions.fromName(name) in BookFileExtensions.recognized) {
                    books.add(CatalogBook(id, relative, document))
                }
            }
        }
        books
    }

    private suspend fun children(tree: Uri, parent: String): List<Child> {
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val context = currentCoroutineContext()
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
        return cancellableQuery { signal ->
            val cursor = safProviderOperation {
                resolver.query(uri, columns, null, null, null, signal)
            } ?: throw IOException("Cannot list Books folder")
            cursor.use {
                val children = mutableListOf<Child>()
                while (it.moveToNext()) {
                    context.ensureActive()
                    if (children.size >= 100_000) throw SourceLimitExceeded()
                    val id = it.getString(0)
                    children.add(
                        Child(
                            id,
                            it.getString(1),
                            SourceDocument(
                                DocumentsContract.buildDocumentUriUsingTree(tree, id).toString(),
                                it.getString(2),
                                if (it.isNull(3)) null else it.getLong(3),
                                if (it.isNull(4)) null else it.getLong(4),
                            ),
                        ),
                    )
                }
                children
            }
        }
    }
    private data class Child(val id: String, val name: String, val document: SourceDocument)
}
