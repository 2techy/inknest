package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.BuildConfig
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkSettingsTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun AboutSettingsContent(
    actions: SettingsActions,
    developerTools: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(InkSpacing.lg)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = InkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InkSpacing.md),
        ) {
            Image(
                painterResource(R.drawable.ic_launcher_foreground),
                null,
                Modifier.size(InkSettingsTokens.choiceMinHeight),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(InkSpacing.xs)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.app_version_author, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column {
            SettingsRow(stringResource(R.string.developer_website), onClick = actions.website)
            SettingsRow(stringResource(R.string.notices_title), onClick = {
                actions.openPage(SettingsPage.Notices)
            })
            SettingsRow(stringResource(R.string.review_setup), onClick = actions.reviewSetup)
        }
        developerTools()
    }
}
