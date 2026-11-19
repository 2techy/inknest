package dev.twotechy.inknest.platform.updates

import android.content.ActivityNotFoundException
import android.content.Context

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
