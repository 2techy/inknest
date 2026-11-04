package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.data.local.userPreferencesDataStore
import dev.twotechy.inknest.data.repository.AssetAcknowledgementsRepository
import dev.twotechy.inknest.data.repository.DataStoreUserPreferencesRepository
import dev.twotechy.inknest.data.repository.DeviceInstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReaderProbeRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.ReadingStatsRepository
import dev.twotechy.inknest.data.repository.SafSourceSettingsRepository
import dev.twotechy.inknest.data.source.AndroidInstalledAppsDataSource
import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.SafSourceAccess
import dev.twotechy.inknest.data.source.SafTreeReader
import dev.twotechy.inknest.data.source.stats.StatisticsSnapshotProbe
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.LauncherModeController
import dev.twotechy.inknest.platform.PackageChangeReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Created once by InkNestApplication; both build types use the same runtime data sources.
internal fun createRuntimeContainer(
    context: Context,
    reading: ReadingRepository,
    stats: ReadingStatsRepository,
): AppContainer {
    val appContext = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val preferences = DataStoreUserPreferencesRepository(appContext.userPreferencesDataStore)
    val apps =
        DeviceInstalledAppsRepository(
            AndroidInstalledAppsDataSource(appContext),
            preferences,
            scope,
        )
    val files = SafTreeReader(appContext.contentResolver)
    val readerProbe = ReaderProbeRepository(
        files,
        books = BookProbeDataSource(files, appContext.cacheDir),
        stats = StatisticsSnapshotProbe(files, appContext.cacheDir),
    )
    val sourceConfig = ReadingSourceConfigRepository(appContext.userPreferencesDataStore)
    val sourceAccess = SafSourceAccess(appContext.contentResolver)
    val realSources =
        SafSourceSettingsRepository(sourceConfig, sourceAccess, readerProbe, files, scope)
    val icons = AppIconLoader(appContext)
    PackageChangeReceiver(apps, scope, icons::clear).register(appContext)
    return AppContainer(
        readingRepository = reading,
        installedAppsRepository = apps,
        readingStatsRepository = stats,
        userPreferencesRepository = preferences,
        appLauncher = AppLauncher(appContext),
        appIconLoader = icons,
        launcherModeController = LauncherModeController(appContext),
        sourceSettingsRepository = realSources,
        readingSourceConfigRepository = sourceConfig,
        readerProbeRepository = readerProbe,
        safSourceAccess = sourceAccess,
        acknowledgementsRepository = AssetAcknowledgementsRepository(appContext.assets),
    )
}
