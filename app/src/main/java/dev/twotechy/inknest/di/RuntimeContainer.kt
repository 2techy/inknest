package dev.twotechy.inknest.di

import android.content.Context
import dev.twotechy.inknest.BuildConfig
import dev.twotechy.inknest.data.local.preferences.DataStoreAppUpdateStore
import dev.twotechy.inknest.data.local.preferences.userPreferencesDataStore
import dev.twotechy.inknest.data.repository.acknowledgements.impl.AssetAcknowledgementsRepository
import dev.twotechy.inknest.data.repository.apps.impl.AndroidInstalledAppsRepository
import dev.twotechy.inknest.data.repository.preferences.impl.DataStoreUserPreferencesRepository
import dev.twotechy.inknest.data.repository.reading.ForegroundRefreshCoordinator
import dev.twotechy.inknest.data.repository.updates.AppUpdateRepository
import dev.twotechy.inknest.data.source.apps.impl.AndroidInstalledAppsDataSource
import dev.twotechy.inknest.data.source.updates.impl.GitHubReleaseDataSource
import dev.twotechy.inknest.data.source.updates.impl.HttpsUpdateTransport
import dev.twotechy.inknest.platform.apps.AppLauncher
import dev.twotechy.inknest.platform.apps.LauncherModeController
import dev.twotechy.inknest.platform.apps.PackageChangeReceiver
import dev.twotechy.inknest.platform.icons.AndroidAppIconDecoder
import dev.twotechy.inknest.platform.icons.AppIconLoader
import dev.twotechy.inknest.platform.tracing.AppOperationTrace
import dev.twotechy.inknest.platform.updates.AndroidUpdateApkVerifier
import dev.twotechy.inknest.platform.updates.AppUpdateInstaller
import dev.twotechy.inknest.platform.updates.PrivateAppUpdateFiles
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
        AndroidInstalledAppsRepository(
            AndroidInstalledAppsDataSource(appContext),
            preferences,
            scope,
        )
    val readerData = ReadingDataContainer(appContext, scope)
    val releases = GitHubReleaseDataSource(HttpsUpdateTransport())
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
    val icons = AppIconLoader(AndroidAppIconDecoder(appContext))
    PackageChangeReceiver(apps, scope, icons::clear).register(appContext)
    return AppContainer(
        appUpdateRepository = updates,
        appUpdateInstaller = AppUpdateInstaller(appContext),
        readingRepository = readerData.reading,
        foregroundRefreshCoordinator = ForegroundRefreshCoordinator(
            readerData.reading,
            readerData.statistics,
            scope,
            AppOperationTrace.tracer,
        ),
        libraryRepository = readerData.library,
        readingOpenRepository = readerData.opening,
        readingItemDispatcher = readerData.dispatcher,
        bookDetailsRepository = readerData.bookDetails,
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
