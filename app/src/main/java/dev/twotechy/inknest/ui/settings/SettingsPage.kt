package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.model.DisplayMode
import dev.twotechy.inknest.model.ThirdPartyLicense

internal enum class SettingsPage(val title: Int) {
    Overview(R.string.settings_title),
    ReadingData(R.string.reading_data_title),
    Appearance(R.string.appearance_title),
    Launcher(R.string.launcher_title),
    About(R.string.about_title),
    Updates(R.string.updates_title),
    Notices(R.string.notices_title),
    LuaJLicense(R.string.license_luaj),
    MaterialSymbolsLicense(R.string.license_material_symbols),
    CommonMarkLicense(R.string.license_commonmark),
    ;

    val license: ThirdPartyLicense? get() = when (this) {
        LuaJLicense -> ThirdPartyLicense.LuaJ
        MaterialSymbolsLicense -> ThirdPartyLicense.MaterialSymbols
        CommonMarkLicense -> ThirdPartyLicense.CommonMark
        else -> null
    }

    val parent: SettingsPage get() = when (this) {
        Notices, Updates -> About
        LuaJLicense, MaterialSymbolsLicense, CommonMarkLicense -> Notices
        else -> Overview
    }
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
    val updates: UpdateSettingsActions = UpdateSettingsActions(),
)

internal data class UpdateSettingsActions(
    val check: () -> Unit = {},
    val automatic: (Boolean) -> Unit = {},
    val download: () -> Unit = {},
    val cancel: () -> Unit = {},
    val install: () -> Unit = {},
    val releasePage: () -> Unit = {},
    val openLink: (String) -> Unit = {},
)
