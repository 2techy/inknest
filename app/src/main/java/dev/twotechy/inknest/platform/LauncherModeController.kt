package dev.twotechy.inknest.platform

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

internal class LauncherModeController(private val context: Context) {

    fun isDefaultLauncher(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }

        // Fallback for pre-Q or when RoleManager is unavailable
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun createRequestRoleIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return null
    }

    fun createHomeSettingsIntent(): Intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    fun createApplicationDetailsSettingsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    fun openHomeSettings(): Boolean {
        val candidates = listOf(
            createHomeSettingsIntent(),
            createApplicationDetailsSettingsIntent(),
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        for (intent in candidates) {
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // OEM readers do not necessarily implement every Settings action.
            } catch (_: SecurityException) {
                // Try the next supported entry point.
            }
        }
        return false
    }
}
