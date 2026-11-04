package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.data.local.reading.ReadingCache
import dev.twotechy.inknest.data.local.reading.ReadingDatabase
import dev.twotechy.inknest.data.local.userPreferencesDataStore
import dev.twotechy.inknest.data.repository.AssetAcknowledgementsRepository
import dev.twotechy.inknest.data.repository.CachedReadingRepository
import dev.twotechy.inknest.data.repository.DataStoreUserPreferencesRepository
import dev.twotechy.inknest.data.repository.DeviceInstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReaderProbeRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.SafSourceSettingsRepository
import dev.twotechy.inknest.data.repository.UnconnectedReadingStatsRepository
import dev.twotechy.inknest.data.source.AndroidInstalledAppsDataSource
import dev.twotechy.inknest.data.source.BookProbeDataSource
import dev.twotechy.inknest.data.source.KoreaderCurrentSource
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
internal fun createRuntimeContainer(context: Context): AppContainer {
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
    val books = BookProbeDataSource(files, appContext.cacheDir)
    val readerProbe = ReaderProbeRepository(
        files,
        books = books,
        stats = StatisticsSnapshotProbe(files, appContext.cacheDir),
    )
    val sourceConfig = ReadingSourceConfigRepository(appContext.userPreferencesDataStore)
    val sourceAccess = SafSourceAccess(appContext.contentResolver)
    val database = ReadingDatabase.create(appContext)
    val reading =
        CachedReadingRepository(
            sourceConfig,
            ReadingCache(database.readingDao()),
            KoreaderCurrentSource(files, books, access = sourceAccess),
            scope,
        )
    val realSources =
        SafSourceSettingsRepository(
            sourceConfig,
            sourceAccess,
            reading,
            appContext.getDatabasePath("reading.db"),
            files,
            scope,
        )
    val icons = AppIconLoader(appContext)
    PackageChangeReceiver(apps, scope, icons::clear).register(appContext)
    return AppContainer(
        readingRepository = reading,
        installedAppsRepository = apps,
        readingStatsRepository = UnconnectedReadingStatsRepository(),
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
