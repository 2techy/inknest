package dev.twotechy.inknest.platform.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent

internal class AppLauncher(private val context: Context) {

    fun openAppInfo(componentId: String): Boolean {
        val component = ComponentName.unflattenFromString(componentId) ?: return false
        if (component.packageName.isEmpty()) return false
        val intent = createApplicationDetailsSettingsIntent(component.packageName)
        return startActivity(intent) == LaunchResult.Success
    }

    fun launch(componentId: String): LaunchResult {
        val parts = componentId.split("/", limit = 2)
        if (parts.size != 2) return LaunchResult.Unavailable

        val packageName = parts[0]
        val className = parts[1]

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(packageName, className)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }

        return startActivity(intent)
    }

    private fun startActivity(intent: Intent): LaunchResult = try {
        context.startActivity(intent)
        LaunchResult.Success
    } catch (_: ActivityNotFoundException) {
        LaunchResult.Unavailable
    } catch (securityException: SecurityException) {
        LaunchResult.Denied(securityException.message ?: "Permission denied to launch app")
    }
}
