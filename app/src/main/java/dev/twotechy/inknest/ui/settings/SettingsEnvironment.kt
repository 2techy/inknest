package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.platform.language.AppLanguageController
import dev.twotechy.inknest.platform.updates.AppUpdateInstaller
import dev.twotechy.inknest.ui.components.LauncherActions

internal data class SettingsEnvironment(
    val launcher: LauncherActions,
    val language: AppLanguageController,
    val isDefaultLauncher: Boolean,
    val updateInstaller: AppUpdateInstaller? = null,
)
