package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.BuildConfig
import dev.twotechy.inknest.data.local.DataStoreAppUpdateStore
import dev.twotechy.inknest.data.local.userPreferencesDataStore
import dev.twotechy.inknest.data.repository.AppUpdateRepository
import dev.twotechy.inknest.data.repository.AssetAcknowledgementsRepository
import dev.twotechy.inknest.data.repository.DataStoreUserPreferencesRepository
import dev.twotechy.inknest.data.repository.DeviceInstalledAppsRepository
import dev.twotechy.inknest.data.repository.ForegroundRefreshCoordinator
import dev.twotechy.inknest.data.source.AndroidInstalledAppsDataSource
import dev.twotechy.inknest.data.source.GitHubReleaseSource
import dev.twotechy.inknest.data.source.HttpsUpdateTransport
import dev.twotechy.inknest.platform.AndroidAppIconSource
import dev.twotechy.inknest.platform.AndroidUpdateApkVerifier
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.AppUpdateInstaller
import dev.twotechy.inknest.platform.LauncherModeController
import dev.twotechy.inknest.platform.PackageChangeReceiver
import dev.twotechy.inknest.platform.PrivateAppUpdateFiles
import java.io.File
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
    val releases = GitHubReleaseSource(HttpsUpdateTransport())
    val updates = AppUpdateRepository(
        releases,
        DataStoreAppUpdateStore(appContext.userPreferencesDataStore),
        PrivateAppUpdateFiles(
            File(appContext.filesDir, "updates"),
            releases,
            AndroidUpdateApkVerifier(appContext),
        ),
        scope,
        BuildConfig.VERSION_NAME,
        automaticAllowed = BuildConfig.BUILD_TYPE == "release",
    )
    val icons = AppIconLoader(AndroidAppIconSource(appContext))
    PackageChangeReceiver(apps, scope, icons::clear).register(appContext)
    return AppContainer(
        appUpdateRepository = updates,
        appUpdateInstaller = AppUpdateInstaller(appContext),
        readingRepository = readerData.reading,
        foregroundRefreshCoordinator = ForegroundRefreshCoordinator(
            readerData.reading,
            readerData.statistics,
            scope,
        ),
        libraryRepository = readerData.library,
        readingOpenRepository = readerData.opening,
        readingItemDispatcher = readerData.dispatcher,
        installedAppsRepository = apps,
        readingStatsRepository = readerData.statistics,
        statisticsRepository = readerData.statistics,
        statisticsClock = readerData.statisticsClock,
        userPreferencesRepository = preferences,
        appLauncher = AppLauncher(appContext),
        appIconLoader = icons,
        bookCoverLoader = readerData.coverLoader,
        launcherModeController = LauncherModeController(appContext),
        sourceSettingsRepository = readerData.settings,
        readingSourceConfigRepository = readerData.config,
        safSourceAccess = readerData.access,
        acknowledgementsRepository = AssetAcknowledgementsRepository(appContext.assets),
    )
}
