package dev.twotechy.inknest.ui.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import dev.twotechy.inknest.data.repository.ReaderProbeRepository
import dev.twotechy.inknest.model.AppInfo
import dev.twotechy.inknest.model.ReadingSourceConfig
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.LaunchResult
import kotlinx.coroutines.CancellationException

internal class SourceProbeLauncher(
    private val context: Context,
    private val apps: AppLauncher,
    private val reader: ReaderProbeRepository,
) {
    fun version(app: AppInfo?): String? {
        val packageName = app?.componentId?.substringBefore('/') ?: return null
        return try {
            context.packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun launchApp(app: AppInfo?): Boolean =
        app?.let { apps.launch(it.componentId) } == LaunchResult.Success

    suspend fun launchBook(config: ReadingSourceConfig, sourcePath: String): Boolean = try {
        // Resolve again: a previous diagnostic result may reference a removed file or revoked grant.
        val book = reader.resolveForOpen(config, sourcePath)
        val uri = Uri.parse(book.document.uri)
        val mime = if (book.relative.last().endsWith(".epub", true)) {
            "application/epub+zip"
        } else {
            book.document.mime ?: "application/octet-stream"
        }
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                setPackage(checkNotNull(config.sourcePackage))
                clipData = ClipData.newRawUri("Book", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }
}
