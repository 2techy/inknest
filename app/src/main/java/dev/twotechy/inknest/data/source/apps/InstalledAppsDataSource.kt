package dev.twotechy.inknest.data.source.apps

import dev.twotechy.inknest.model.AppInfo

interface InstalledAppsDataSource {
    suspend fun loadInstalledApps(): List<AppInfo>
}
