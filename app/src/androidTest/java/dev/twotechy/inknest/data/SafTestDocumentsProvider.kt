package dev.twotechy.inknest.data

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider

internal class SafTestDocumentsProvider : DocumentsProvider() {
    enum class MissingMetadata { Empty, Null, Rejected }

    data class Entry(
        val id: String,
        val parent: String?,
        val name: String,
        val mime: String = "application/epub+zip",
    )

    val entries = mutableMapOf<String, Entry>()
    val metadataQueries = mutableListOf<String>()
    val childQueries = mutableListOf<String>()
    var granted = true
    var queryFailure: RuntimeException? = null
    var nullMetadata = false
    var missingMetadata = MissingMetadata.Empty

    override fun onCreate() = true
    override fun queryRoots(projection: Array<out String>?): Cursor =
        MatrixCursor(arrayOf(Root.COLUMN_ROOT_ID))

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean =
        documentId.startsWith(parentDocumentId + if (parentDocumentId == "primary:") "" else "/")

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor? {
        checkGrant()
        metadataQueries += documentId
        queryFailure?.let { throw it }
        if (nullMetadata) return null
        val entry = entries[documentId]
        if (entry == null) {
            when (missingMetadata) {
                MissingMetadata.Empty -> Unit
                MissingMetadata.Null -> return null
                MissingMetadata.Rejected -> throw IllegalArgumentException("Missing ID rejected")
            }
        }
        val columns = projection ?: arrayOf(Document.COLUMN_DOCUMENT_ID)
        return MatrixCursor(columns).apply {
            entry?.let { addRow(columns.map { entry.column(it) }) }
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        checkGrant()
        childQueries += parentDocumentId
        val columns = projection ?: arrayOf(Document.COLUMN_DOCUMENT_ID)
        return MatrixCursor(columns).apply {
            entries.values.filter { it.parent == parentDocumentId }.forEach { entry ->
                addRow(columns.map { entry.column(it) })
            }
        }
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        checkGrant()
        signal?.throwIfCanceled()
        val pipe = ParcelFileDescriptor.createPipe()
        ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write("book".toByteArray()) }
        return pipe[0]
    }

    fun add(id: String, parent: String? = null, name: String = id.substringAfterLast('/')) {
        entries[id] = Entry(id, parent, name)
    }

    private fun checkGrant() {
        if (!granted) throw SecurityException("Grant revoked by provider")
    }

    private fun Entry.column(column: String): Any? = when (column) {
        Document.COLUMN_DOCUMENT_ID -> id
        Document.COLUMN_DISPLAY_NAME -> name
        Document.COLUMN_MIME_TYPE -> mime
        Document.COLUMN_SIZE -> 4L
        Document.COLUMN_LAST_MODIFIED -> 1L
        else -> null
    }
}
