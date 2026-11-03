package dev.twotechy.inknest.data.source

import android.content.Context
import android.content.Intent
import dev.twotechy.inknest.model.AppInfo
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface InstalledAppsDataSource {
    suspend fun loadInstalledApps(): List<AppInfo>
}

internal class AndroidInstalledAppsDataSource(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : InstalledAppsDataSource {

    override suspend fun loadInstalledApps(): List<AppInfo> = withContext(ioDispatcher) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // A failed query must propagate: an empty successful result authorizes pin pruning.
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        val packageVersions = mutableMapOf<String, Long>()

        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }

        val selfPackage = context.packageName

        resolveInfos
            .filter { resolveInfo ->
                // Do not show InkNest itself in the app drawer
                resolveInfo.activityInfo.packageName != selfPackage
            }
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                val componentId = "${activityInfo.packageName}/${activityInfo.name}"
                val label = resolveInfo.loadLabel(packageManager).toString().trim()
                    .ifEmpty { activityInfo.packageName }
                val version = packageVersions.getOrPut(activityInfo.packageName) {
                    packageManager.getPackageInfo(activityInfo.packageName, 0).lastUpdateTime
                }
                AppInfo(componentId = componentId, label = label, iconVersion = version)
            }
            .distinctBy(AppInfo::componentId)
            .sortedWith { a, b ->
                val labelCompare = collator.compare(a.label, b.label)
                if (labelCompare != 0) labelCompare else a.componentId.compareTo(b.componentId)
            }
    }
}
