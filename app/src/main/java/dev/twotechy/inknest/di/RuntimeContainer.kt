package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.data.local.userPreferencesDataStore
import dev.twotechy.inknest.data.repository.AssetAcknowledgementsRepository
import dev.twotechy.inknest.data.repository.DataStoreUserPreferencesRepository
import dev.twotechy.inknest.data.repository.DeviceInstalledAppsRepository
import dev.twotechy.inknest.data.source.AndroidInstalledAppsDataSource
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.LauncherModeController
import dev.twotechy.inknest.platform.PackageChangeReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Created once by InkNestApplication; both build types use the same runtime data sources.
internal fun createRuntimeContainer(context: Context): AppContainer {
    val appContext = context.applicationContext
    cleanReadingTemporaryFiles(appContext)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val preferences = DataStoreUserPreferencesRepository(appContext.userPreferencesDataStore)
    val apps =
        DeviceInstalledAppsRepository(
            AndroidInstalledAppsDataSource(appContext),
            preferences,
            scope,
        )
    val readerData = ReadingDataContainer(appContext, scope)
    val icons = AppIconLoader(appContext)
    PackageChangeReceiver(apps, scope, icons::clear).register(appContext)
    return AppContainer(
        readingRepository = readerData.reading,
        libraryRepository = readerData.library,
        readingItemOpener = readerData.opener,
        installedAppsRepository = apps,
        readingStatsRepository = readerData.statistics,
        statisticsRepository = readerData.statistics,
        userPreferencesRepository = preferences,
        appLauncher = AppLauncher(appContext),
        appIconLoader = icons,
        launcherModeController = LauncherModeController(appContext),
        sourceSettingsRepository = readerData.settings,
        readingSourceConfigRepository = readerData.config,
        safSourceAccess = readerData.access,
        acknowledgementsRepository = AssetAcknowledgementsRepository(appContext.assets),
    )
}
