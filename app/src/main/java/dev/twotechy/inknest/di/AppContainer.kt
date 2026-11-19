package dev.twotechy.inknest.di

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.twotechy.inknest.data.repository.acknowledgements.AcknowledgementsRepository
import dev.twotechy.inknest.data.repository.apps.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.books.BookDetailsRepository
import dev.twotechy.inknest.data.repository.library.LibraryRepository
import dev.twotechy.inknest.data.repository.preferences.UserPreferencesRepository
import dev.twotechy.inknest.data.repository.reading.ForegroundRefreshCoordinator
import dev.twotechy.inknest.data.repository.reading.ReadingOpenRepository
import dev.twotechy.inknest.data.repository.reading.ReadingRepository
import dev.twotechy.inknest.data.repository.reading.ReadingStatsRepository
import dev.twotechy.inknest.data.repository.sources.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.sources.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.sources.impl.UnconnectedSourceSettingsRepository
import dev.twotechy.inknest.data.repository.statistics.StatisticsRepository
import dev.twotechy.inknest.data.repository.updates.AppUpdateRepository
import dev.twotechy.inknest.data.source.saf.SafSourceAccess
import dev.twotechy.inknest.platform.apps.AppLauncher
import dev.twotechy.inknest.platform.apps.LauncherModeController
import dev.twotechy.inknest.platform.clock.StatisticsClock
import dev.twotechy.inknest.platform.covers.BookCoverLoader
import dev.twotechy.inknest.platform.icons.AppIconLoader
import dev.twotechy.inknest.platform.language.AppLanguageController
import dev.twotechy.inknest.platform.reading.ReadingItemDispatcher
import dev.twotechy.inknest.platform.updates.AppUpdateInstaller
import dev.twotechy.inknest.ui.AppViewModel
import dev.twotechy.inknest.ui.apps.AppsViewModel
import dev.twotechy.inknest.ui.home.HomeViewModel
import dev.twotechy.inknest.ui.library.LibraryViewModel
import dev.twotechy.inknest.ui.library.details.BookDetailsViewModel
import dev.twotechy.inknest.ui.onboarding.OnboardingViewModel
import dev.twotechy.inknest.ui.settings.ReleaseNotesParser
import dev.twotechy.inknest.ui.settings.SettingsViewModel
import dev.twotechy.inknest.ui.stats.StatsViewModel

// Application-scoped dependencies; build-specific wiring lives in debug/release.
internal class AppContainer(
    val readingRepository: ReadingRepository,
    val foregroundRefreshCoordinator: ForegroundRefreshCoordinator,
    val libraryRepository: LibraryRepository,
    val installedAppsRepository: InstalledAppsRepository,
    val readingStatsRepository: ReadingStatsRepository,
    val userPreferencesRepository: UserPreferencesRepository,
    val appLauncher: AppLauncher,
    val appIconLoader: AppIconLoader,
    val bookCoverLoader: BookCoverLoader,
    val launcherModeController: LauncherModeController,
    val acknowledgementsRepository: AcknowledgementsRepository,
    val statisticsRepository: StatisticsRepository,
    val statisticsClock: StatisticsClock,
    val sourceSettingsRepository: SourceSettingsRepository = UnconnectedSourceSettingsRepository(),
    val readingSourceConfigRepository: ReadingSourceConfigRepository? = null,
    val safSourceAccess: SafSourceAccess? = null,
    val readingOpenRepository: ReadingOpenRepository,
    val readingItemDispatcher: ReadingItemDispatcher,
    val appUpdateRepository: AppUpdateRepository? = null,
    val appUpdateInstaller: AppUpdateInstaller? = null,
    val bookDetailsRepository: BookDetailsRepository,
) {
    val appLanguageController = AppLanguageController()

    val appViewModelFactory = viewModelFactory {
        initializer {
            AppViewModel(
                foregroundRefreshCoordinator,
                userPreferencesRepository,
                installedAppsRepository,
                appUpdateRepository,
            )
        }
    }
    val statsViewModelFactory = viewModelFactory {
        initializer {
            StatsViewModel(statisticsRepository, createSavedStateHandle(), statisticsClock)
        }
    }
    val settingsViewModelFactory = viewModelFactory {
        initializer {
            SettingsViewModel(
                userPreferencesRepository,
                sourceSettingsRepository,
                acknowledgementsRepository,
                createSavedStateHandle(),
                installedAppsRepository,
                appUpdateRepository,
                notesParser = ReleaseNotesParser(),
            )
        }
    }
    val onboardingViewModelFactory = viewModelFactory {
        initializer {
            OnboardingViewModel(
                userPreferencesRepository,
                createSavedStateHandle(),
                sourceSettingsRepository,
                installedAppsRepository,
            )
        }
    }

    val homeViewModelFactory = viewModelFactory {
        initializer {
            HomeViewModel(
                readingRepository,
                installedAppsRepository,
                readingStatsRepository,
                readingOpenRepository,
                statisticsRepository,
                statisticsClock,
            )
        }
    }

    val libraryViewModelFactory = viewModelFactory {
        initializer {
            LibraryViewModel(libraryRepository, createSavedStateHandle(), readingOpenRepository)
        }
    }

    val bookDetailsViewModelFactory = viewModelFactory {
        initializer {
            BookDetailsViewModel(
                bookDetailsRepository,
                createSavedStateHandle(),
                statisticsClock,
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
