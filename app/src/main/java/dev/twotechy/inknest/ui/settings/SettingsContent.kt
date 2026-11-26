package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.sources.SourceStatus

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
                R.drawable.ic_settings_display,
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
