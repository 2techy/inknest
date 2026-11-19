package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.R
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
