package dev.twotechy.inknest.di

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.twotechy.inknest.data.repository.AcknowledgementsRepository
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.ReaderProbeRepository
import dev.twotechy.inknest.data.repository.ReadingRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.ReadingStatsRepository
import dev.twotechy.inknest.data.repository.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.UnconnectedSourceSettingsRepository
import dev.twotechy.inknest.data.repository.UserPreferencesRepository
import dev.twotechy.inknest.data.source.SafSourceAccess
import dev.twotechy.inknest.platform.AppIconLoader
import dev.twotechy.inknest.platform.AppLauncher
import dev.twotechy.inknest.platform.LauncherModeController
import dev.twotechy.inknest.ui.apps.AppsViewModel
import dev.twotechy.inknest.ui.home.HomeViewModel
import dev.twotechy.inknest.ui.onboarding.OnboardingViewModel
import dev.twotechy.inknest.ui.settings.SettingsViewModel

// Application-scoped dependencies; build-specific wiring lives in debug/release.
internal class AppContainer(
    val readingRepository: ReadingRepository,
    val installedAppsRepository: InstalledAppsRepository,
    val readingStatsRepository: ReadingStatsRepository,
    val userPreferencesRepository: UserPreferencesRepository,
    val appLauncher: AppLauncher,
    val appIconLoader: AppIconLoader,
    val launcherModeController: LauncherModeController,
    val acknowledgementsRepository: AcknowledgementsRepository,
    val sourceSettingsRepository: SourceSettingsRepository = UnconnectedSourceSettingsRepository(),
    val readingSourceConfigRepository: ReadingSourceConfigRepository? = null,
    val safSourceAccess: SafSourceAccess? = null,
    val readerProbeRepository: ReaderProbeRepository? = null,
) {
    val settingsViewModelFactory = viewModelFactory {
        initializer {
            SettingsViewModel(
                userPreferencesRepository,
                sourceSettingsRepository,
                acknowledgementsRepository,
                createSavedStateHandle(),
                installedAppsRepository,
            )
        }
    }
    val onboardingViewModelFactory = viewModelFactory {
        initializer { OnboardingViewModel(userPreferencesRepository, createSavedStateHandle()) }
    }

    val homeViewModelFactory = viewModelFactory {
        initializer {
            HomeViewModel(
                readingRepository,
                installedAppsRepository,
                readingStatsRepository,
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
