package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.data.local.userPreferencesDataStore
import dev.twotechy.inknest.data.preview.HomePreviewController
import dev.twotechy.inknest.data.preview.SourcePreviewController
import dev.twotechy.inknest.data.repository.AssetAcknowledgementsRepository
import dev.twotechy.inknest.data.repository.DataStoreUserPreferencesRepository
import dev.twotechy.inknest.data.repository.DeviceInstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReaderProbeRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.ReadingStatsRepository
import dev.twotechy.inknest.data.repository.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.UnconnectedSourceSettingsRepository
import dev.twotechy.inknest.data.source.AndroidInstalledAppsDataSource
import dev.twotechy.inknest.data.source.SafSourceAccess
import dev.twotechy.inknest.data.source.SafTreeReader
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.LauncherModeController
import dev.twotechy.inknest.platform.PackageChangeReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Created once by InkNestApplication. Only reading/preview wiring differs by build type.
internal fun createRuntimeContainer(
    context: Context,
    reading: ReadingRepository,
    stats: ReadingStatsRepository,
    preview: HomePreviewController? = null,
    sources: SourceSettingsRepository = UnconnectedSourceSettingsRepository(),
    sourcePreview: SourcePreviewController? = null,
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
        homePreviewController = preview,
        sourceSettingsRepository = sources,
        sourcePreviewController = sourcePreview,
        readingSourceConfigRepository = ReadingSourceConfigRepository(
            appContext.userPreferencesDataStore,
        ),
        readerProbeRepository = ReaderProbeRepository(SafTreeReader(appContext.contentResolver)),
        safSourceAccess = SafSourceAccess(appContext.contentResolver),
        acknowledgementsRepository = AssetAcknowledgementsRepository(appContext.assets),
    )
}
