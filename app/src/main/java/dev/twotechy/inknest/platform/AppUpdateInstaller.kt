package dev.twotechy.inknest.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

internal enum class UpdateInstallResult { Opened, PermissionRequired, Unavailable }

internal interface UpdateInstallPlatform {
    fun canInstall(): Boolean
    fun requestPermission()
    fun install(fileName: String)
}

internal class AppUpdateInstaller(private val platform: UpdateInstallPlatform) {
    constructor(context: Context) : this(AndroidUpdateInstallPlatform(context))

    fun open(fileName: String): UpdateInstallResult {
        if (fileName != "update.apk") return UpdateInstallResult.Unavailable
        return try {
            if (!platform.canInstall()) {
                platform.requestPermission()
                UpdateInstallResult.PermissionRequired
            } else {
                platform.install(fileName)
                UpdateInstallResult.Opened
            }
        } catch (_: ActivityNotFoundException) {
            UpdateInstallResult.Unavailable
        } catch (_: SecurityException) {
            UpdateInstallResult.Unavailable
        } catch (_: IllegalArgumentException) {
            UpdateInstallResult.Unavailable
        }
    }
}

internal class AndroidUpdateInstallPlatform(private val context: Context) : UpdateInstallPlatform {
    override fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    override fun requestPermission() {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    override fun install(fileName: String) {
        val file = File(context.filesDir, "updates/$fileName")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
