package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import dev.twotechy.inknest.BuildConfig
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkLayoutTokens
import dev.twotechy.inknest.ui.theme.InkSettingsTokens
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun AboutSettingsContent(actions: SettingsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(InkLayoutTokens.sectionGap)) {
        AboutIdentity()
        AboutUpdates()
        Column {
            SettingsRow(stringResource(R.string.developer_website), onClick = actions.website)
            SettingsRow(stringResource(R.string.review_setup), onClick = actions.reviewSetup)
            SettingsRow(stringResource(R.string.notices_title), onClick = {
                actions.openPage(SettingsPage.Notices)
            })
        }
    }
}

@Composable
private fun AboutIdentity() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = InkSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(InkSpacing.sm),
    ) {
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            null,
            Modifier.size(InkSettingsTokens.choiceMinHeight),
        )
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.app_version_author, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AboutUpdates() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(InkSpacing.xs),
    ) {
        SettingsCommand(stringResource(R.string.check_for_updates), enabled = false, onClick = {})
        Text(
            stringResource(R.string.updates_coming_soon),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
