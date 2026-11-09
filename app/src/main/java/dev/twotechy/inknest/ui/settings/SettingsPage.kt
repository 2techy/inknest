package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.model.DisplayMode

internal enum class SettingsPage(val title: Int) {
    Overview(R.string.settings_title),
    ReadingData(R.string.reading_data_title),
    Appearance(R.string.appearance_title),
    Launcher(R.string.launcher_title),
    About(R.string.about_title),
    Notices(R.string.notices_title),
    ;

    val parent: SettingsPage get() = if (this == Notices) About else Overview
}

internal data class SettingsActions(
    val openPage: (SettingsPage) -> Unit,
    val back: () -> Unit,
    val displayMode: (DisplayMode) -> Unit,
    val language: () -> Unit,
    val showHeatmap: (Boolean) -> Unit,
    val showStats: (Boolean) -> Unit,
    val showPins: (Boolean) -> Unit,
    val connect: (SourceFolder) -> Unit,
    val sourceApp: (String) -> Unit,
    val replaceBookFolder: (String) -> Unit,
    val removeBookFolder: (String) -> Unit,
    val disconnect: () -> Unit,
    val refresh: () -> Unit,
    val clearCache: () -> Unit,
    val requestLauncher: () -> Unit,
    val systemSettings: () -> Unit,
    val reviewSetup: () -> Unit,
    val website: () -> Unit,
    val retry: () -> Unit,
    val dismiss: () -> Unit,
    val retryAcknowledgements: () -> Unit,
)
