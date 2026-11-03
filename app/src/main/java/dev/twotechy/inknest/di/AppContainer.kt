package dev.twotechy.inknest.di

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.twotechy.inknest.data.preview.HomePreviewController
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingStatsRepository
import dev.twotechy.inknest.data.repository.UserPreferencesRepository
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.LauncherModeController
import dev.twotechy.inknest.ui.apps.AppsViewModel
import dev.twotechy.inknest.ui.home.HomeViewModel

// Application-scoped dependencies; build-specific wiring lives in debug/release.
internal class AppContainer(
    val readingRepository: ReadingRepository,
    val installedAppsRepository: InstalledAppsRepository,
    val readingStatsRepository: ReadingStatsRepository,
    val userPreferencesRepository: UserPreferencesRepository,
    val appLauncher: AppLauncher,
    val appIconLoader: AppIconLoader,
    val launcherModeController: LauncherModeController,
    val homePreviewController: HomePreviewController? = null,
) {
    val homeViewModelFactory = viewModelFactory {
        initializer {
            HomeViewModel(
                readingRepository,
                installedAppsRepository,
                readingStatsRepository,
                createSavedStateHandle(),
                homePreviewController,
            )
        }
    }

    val appsViewModelFactory = viewModelFactory {
        initializer {
            AppsViewModel(
                installedAppsRepository,
                createSavedStateHandle(),
            )
        }
    }
}
