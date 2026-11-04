package dev.twotechy.inknest.platform

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.MissingSourceFile
import dev.twotechy.inknest.data.source.UnresolvedBookPath
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingOpenMode
import dev.twotechy.inknest.model.ReadingSourceConfig
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal enum class OpenResult {
    Success,
    SourceChanged,
    MissingApp,
    MissingFile,
    PermissionLost,
    Unavailable,
}

// Shared by reading routes. Resolves the original document again at the point of use.
internal class ReadingItemOpener(
    private val context: Context,
    private val config: ReadingSourceConfigRepository,
    private val books: BookProbeDataSource,
) {
    fun isDirect(selected: ReadingSourceConfig): Boolean =
        selected.openMode == ReadingOpenMode.DirectConfirmed &&
            selected.verifiedAppVersion != null &&
            selected.verifiedAppVersion == version(selected.sourcePackage)

    private fun version(packageName: String?): String? = try {
        packageName?.let { context.packageManager.getPackageInfo(it, 0).versionName }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    suspend fun open(item: ReadingItemId): OpenResult {
        val selected = config.config.first()
        if (selected.sourceId != item.sourceId) return OpenResult.SourceChanged
        return try {
            val intent = withContext(Dispatchers.IO) { prepare(selected, item) }
                ?: return OpenResult.MissingApp
            if (config.config.first() != selected) return OpenResult.SourceChanged
            context.startActivity(intent)
            OpenResult.Success
        } catch (_: MissingSourceFile) {
            OpenResult.MissingFile
        } catch (_: UnresolvedBookPath) {
            OpenResult.PermissionLost
        } catch (_: SecurityException) {
            OpenResult.PermissionLost
        } catch (_: ActivityNotFoundException) {
            OpenResult.MissingApp
        } catch (_: IOException) {
            OpenResult.Unavailable
        }
    }

    private suspend fun prepare(selected: ReadingSourceConfig, item: ReadingItemId): Intent? {
        val packageName = selected.sourcePackage ?: return null
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return null
        if (!isDirect(selected)) return launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val book = books.resolveIdentity(selected, item.path)
        val uri = Uri.parse(book.document.uri)
        val mime = if (book.relative.last().endsWith(".epub", true)) {
            "application/epub+zip"
        } else {
            book.document.mime ?: "application/octet-stream"
        }
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            setPackage(packageName)
            clipData = ClipData.newRawUri("Book", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
