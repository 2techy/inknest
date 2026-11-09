package dev.twotechy.inknest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.twotechy.inknest.R
import dev.twotechy.inknest.ui.theme.InkSpacing

@Composable
internal fun AcknowledgementsContent(state: AcknowledgementsUiState, retry: () -> Unit) {
    when (state) {
        AcknowledgementsUiState.NotLoaded, AcknowledgementsUiState.Loading ->
            Text(stringResource(R.string.loading_notices))

        is AcknowledgementsUiState.Content -> Text(
            state.text,
            style = MaterialTheme.typography.bodyMedium,
        )

        AcknowledgementsUiState.Failed -> Column(
            verticalArrangement = Arrangement.spacedBy(InkSpacing.md),
        ) {
            Text(stringResource(R.string.notices_unavailable))
            SettingsAction(stringResource(R.string.retry), onClick = retry)
        }
    }
}

@Composable
internal fun ThirdPartyLicensesContent(openPage: (SettingsPage) -> Unit) {
    Column {
        SettingsRow(
            stringResource(R.string.license_luaj),
            stringResource(R.string.license_mit),
            onClick = { openPage(SettingsPage.LuaJLicense) },
        )
        SettingsRow(
            stringResource(R.string.license_material_symbols),
            stringResource(R.string.license_apache),
            onClick = { openPage(SettingsPage.MaterialSymbolsLicense) },
        )
    }
}

@Composable
internal fun LicenseSettingsContent(
    page: SettingsPage,
    state: SettingsUiState,
    actions: SettingsActions,
) {
    val license = page.license
    if (license == null) {
        ThirdPartyLicensesContent(actions.openPage)
    } else {
        AcknowledgementsContent(
            state.acknowledgements[license] ?: AcknowledgementsUiState.NotLoaded,
            actions.retryAcknowledgements,
        )
    }
}
