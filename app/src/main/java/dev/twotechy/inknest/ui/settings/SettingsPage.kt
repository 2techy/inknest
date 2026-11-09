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
    Notices(R.string.notices_title),
    LuaJLicense(R.string.license_luaj),
    MaterialSymbolsLicense(R.string.license_material_symbols),
    ;

    val license: ThirdPartyLicense? get() = when (this) {
        LuaJLicense -> ThirdPartyLicense.LuaJ
        MaterialSymbolsLicense -> ThirdPartyLicense.MaterialSymbols
        else -> null
    }

    val parent: SettingsPage get() = when (this) {
        Notices -> About
        LuaJLicense, MaterialSymbolsLicense -> Notices
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
)
