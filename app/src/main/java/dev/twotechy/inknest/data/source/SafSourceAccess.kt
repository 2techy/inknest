package dev.twotechy.inknest.data.source

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import dev.twotechy.inknest.model.SourceAccessState
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SafSourceAccess(private val resolver: ContentResolver) {
    suspend fun inspect(raw: String?): SourceAccessState = withContext(Dispatchers.IO) {
        if (raw == null) return@withContext SourceAccessState.NotSelected
        val uri = Uri.parse(raw)
        if (uri.authority != "com.android.externalstorage.documents" ||
            !DocumentsContract.isTreeUri(uri)
        ) {
            return@withContext SourceAccessState.Unsupported
        }
        try {
            if (PrimaryTreePath.parse(DocumentsContract.getTreeDocumentId(uri)) == null) {
                return@withContext SourceAccessState.Unsupported
            }
            if (resolver.persistedUriPermissions.none { it.uri == uri && it.isReadPermission }) {
                return@withContext SourceAccessState.PermissionLost
            }
            val document = DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri),
            )
            cancellableQuery { signal ->
                val cursor = resolver.query(
                    document,
                    arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE),
                    null,
                    null,
                    null,
                    signal,
                ) ?: return@cancellableQuery SourceAccessState.ProviderError
                cursor.use {
                    if (!it.moveToFirst() ||
                        it.getString(0) != DocumentsContract.Document.MIME_TYPE_DIR
                    ) {
                        SourceAccessState.InvalidFolder
                    } else {
                        SourceAccessState.Granted
                    }
                }
            }
        } catch (_: SecurityException) {
            SourceAccessState.PermissionLost
        } catch (_: IllegalArgumentException) {
            SourceAccessState.InvalidFolder
        } catch (_: IOException) {
            SourceAccessState.ProviderError
        }
    }

    suspend fun releaseUnused(uri: String, retained: Set<String>) = withContext(Dispatchers.IO) {
        if (uri in retained) return@withContext
        val parsed = Uri.parse(uri)
        if (resolver.persistedUriPermissions.any { it.uri == parsed && it.isReadPermission }) {
            // Source config is already committed. An expired grant must not turn a successful
            // source change into a false save failure.
            try {
                resolver.releasePersistableUriPermission(
                    parsed,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) { }
        }
    }

    suspend fun persist(uri: Uri, resultFlags: Int) = withContext(Dispatchers.IO) {
        require(resultFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        require(resultFlags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0)
        require(
            uri.authority == "com.android.externalstorage.documents" &&
                DocumentsContract.isTreeUri(uri),
        )
        require(PrimaryTreePath.parse(DocumentsContract.getTreeDocumentId(uri)) != null)
        val alreadyGranted = resolver.persistedUriPermissions.any {
            it.uri == uri &&
                it.isReadPermission
        }
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (inspect(uri.toString()) != SourceAccessState.Granted) {
            if (!alreadyGranted) {
                resolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            error("Directory is not readable")
        }
    }
}
