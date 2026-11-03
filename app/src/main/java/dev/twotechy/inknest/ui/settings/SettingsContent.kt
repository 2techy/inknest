package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.SourceStatus
import dev.twotechy.inknest.model.DisplayMode

@Composable
internal fun SettingsOverview(open: (SettingsPage) -> Unit) {
    Column {
        listOf(
            Triple(
                SettingsPage.ReadingData,
                R.string.reading_data_summary,
                R.drawable.ic_nav_library_outline,
            ),
            Triple(
                SettingsPage.Appearance,
                R.string.appearance_summary,
                R.drawable.ic_settings_outline,
            ),
            Triple(
                SettingsPage.Launcher,
                R.string.launcher_summary,
                R.drawable.ic_nav_home_outline,
            ),
            Triple(SettingsPage.About, R.string.about_summary, R.drawable.ic_settings_info),
        ).forEach { (page, description, icon) ->
            SettingsRow(
                stringResource(page.title),
                stringResource(description),
                icon,
                onClick = { open(page) },
            )
        }
    }
}

@Composable
internal fun DisplayModeChoices(
    mode: DisplayMode,
    enabled: Boolean,
    onMode: (DisplayMode) -> Unit,
) {
    Column {
        DisplayMode.entries.forEach { choice ->
            SettingsChoice(
                stringResource(
                    if (choice == DisplayMode.EInk) {
                        R.string.eink_mode
                    } else {
                        R.string.standard_mode
                    },
                ),
                mode == choice,
                enabled,
                description = stringResource(
                    if (choice ==
                        DisplayMode.EInk
                    ) {
                        R.string.eink_description
                    } else {
                        R.string.standard_description
                    },
                ),
            ) { onMode(choice) }
        }
    }
}

@Composable
internal fun LauncherSettingsContent(
    isDefault: Boolean,
    request: () -> Unit,
    systemSettings: () -> Unit,
) {
    Column {
        SettingsValue(
            stringResource(R.string.default_home_app),
            stringResource(if (isDefault) R.string.app_name else R.string.other_home_app),
        )
        if (!isDefault) {
            SettingsCommand(stringResource(R.string.request_default_launcher), onClick = request)
        }
        SettingsRow(
            stringResource(R.string.open_home_settings),
            stringResource(R.string.default_launcher_description),
            onClick = systemSettings,
        )
    }
}

@Composable
internal fun sourceStatusLabel(status: SourceStatus): String = stringResource(
    when (status) {
        SourceStatus.NotConnected -> R.string.source_not_connected
        SourceStatus.Connected -> R.string.source_connected
        SourceStatus.PermissionLost -> R.string.source_permission_lost
        SourceStatus.Error -> R.string.source_error
    },
)
