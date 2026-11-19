package dev.twotechy.inknest.data.source.saf.impl

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import dev.twotechy.inknest.data.source.io.SourceLimitExceededException
import dev.twotechy.inknest.data.source.io.cancellableQuery
import dev.twotechy.inknest.data.source.saf.DirectSourceDocumentReader
import dev.twotechy.inknest.data.source.saf.MissingSourceFileException
import dev.twotechy.inknest.data.source.saf.PrimaryTreePath
import dev.twotechy.inknest.data.source.saf.SafPermissionSnapshot
import dev.twotechy.inknest.data.source.saf.SourceDocument
import dev.twotechy.inknest.data.source.saf.SourceMetadataCapture
import dev.twotechy.inknest.data.source.saf.SourceMetadataFingerprint
import dev.twotechy.inknest.data.source.saf.hasPersistedReadGrant
import dev.twotechy.inknest.data.source.saf.readableTreeUris
import dev.twotechy.inknest.data.source.saf.safProviderOperation
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SafTreeReader(
    private val resolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val readableTrees: () -> Set<String> = resolver::readableTreeUris,
) : DirectSourceDocumentReader {
    suspend fun permissionSnapshot(): SafPermissionSnapshot = withContext(ioDispatcher) {
        SafPermissionSnapshot(readableTrees())
    }

    override suspend fun <T> read(
        tree: String,
        path: List<String>,
        parse: suspend (InputStream) -> T,
    ): T = withContext(ioDispatcher) {
        val document = describe(tree, path).uri.toUri()
        readUri(document, parse)
    }

    override suspend fun <T> readDocument(
        document: SourceDocument,
        parse: suspend (InputStream) -> T,
    ): T = withContext(ioDispatcher) {
        val uri = document.uri.toUri()
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

    private suspend fun <T> readUri(document: Uri, parse: suspend (InputStream) -> T): T = try {
        readStream(document, parse)
    } catch (error: Exception) {
        currentCoroutineContext()[SourceMetadataCapture]?.fail()
        throw error
    }

    private suspend fun <T> readStream(document: Uri, parse: suspend (InputStream) -> T): T {
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
        withContext(ioDispatcher) {
            val capture = currentCoroutineContext()[SourceMetadataCapture]
                ?: return@withContext describeInTree(tree, path)
            val reference = SourceMetadataFingerprint.Document(tree, path.toList())
            try {
                describeInTree(tree, path).also { capture.record(reference, it) }
            } catch (missing: MissingSourceFileException) {
                capture.record(reference, null)
                throw missing
            } catch (error: Exception) {
                capture.fail()
                throw error
            }
        }

    private suspend fun describeInTree(tree: String, path: List<String>): SourceDocument {
        validatePath(path)
        val uri = checkedTreeUri(tree)
        val rootId = DocumentsContract.getTreeDocumentId(uri)
        val id = rootId + when {
            path.isEmpty() -> ""
            rootId == "primary:" -> path.joinToString("/")
            else -> "/" + path.joinToString("/")
        }
        val root = requireNotNull(PrimaryTreePath.parse(rootId))
        if (PrimaryTreePath.parse(id)?.relativeTo(root) == null) throw IOException("Outside tree")
        val direct = readDirectMetadata(DocumentsContract.buildDocumentUriUsingTree(uri, id))
        if (direct != null) return direct
        val resolved = resolveDocumentId(uri, path)
        return readMetadata(DocumentsContract.buildDocumentUriUsingTree(uri, resolved))
            ?: throw IOException("No metadata")
    }

    private suspend fun readDirectMetadata(document: Uri): SourceDocument? = try {
        readMetadata(document)
    } catch (error: IOException) {
        // OEM enforceTree may reject a missing ID before queryDocument can return an empty row.
        // Exact child traversal distinguishes absence from a failure to read an existing file.
        if (error is MissingSourceFileException ||
            error.cause is IllegalArgumentException
        ) {
            null
        } else {
            throw error
        }
    }

    private fun validatePath(path: List<String>) {
        require(
            path.all {
                it.isNotBlank() && it != "." && it != ".." && '/' !in it &&
                    '\u0000' !in it
            },
        )
    }

    private suspend fun checkedTreeUri(tree: String): Uri {
        val uri = tree.toUri()
        require(
            uri.authority == "com.android.externalstorage.documents" &&
                DocumentsContract.isTreeUri(uri),
        )
        requireNotNull(PrimaryTreePath.parse(DocumentsContract.getTreeDocumentId(uri)))
        if (!hasPersistedReadGrant(uri, readableTrees)) {
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

    private suspend fun readMetadata(document: Uri): SourceDocument? {
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        return cancellableQuery { signal ->
            val cursor = safProviderOperation {
                resolver.query(document, columns, null, null, null, signal)
            }
                ?: return@cancellableQuery null
            cursor.use {
                if (!it.moveToFirst()) throw MissingSourceFileException()
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
                    if (++count > 100_000) throw SourceLimitExceededException()
                    if (it.getString(1) == name) return@cancellableQuery it.getString(0)
                }
            }
            throw MissingSourceFileException()
        }
    }
}
