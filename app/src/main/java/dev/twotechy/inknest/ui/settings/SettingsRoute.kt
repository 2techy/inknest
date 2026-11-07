package dev.twotechy.inknest.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twotechy.inknest.R
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.platform.AppLanguageController
import dev.twotechy.inknest.ui.components.ConfirmationDialog
import dev.twotechy.inknest.ui.components.LauncherActions

private enum class Confirmation { Disconnect, ClearCache }

internal data class SettingsEnvironment(
    val launcher: LauncherActions,
    val language: AppLanguageController,
    val isDefaultLauncher: Boolean,
)

@Composable
internal fun SettingsRoute(
    viewModel: SettingsViewModel,
    page: SettingsPage,
    openPage: (SettingsPage) -> Unit,
    environment: SettingsEnvironment,
    onReviewSetup: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val language = rememberLanguageSelection(environment.language, page)
    var confirmation by remember(page) { mutableStateOf<Confirmation?>(null) }
    SourceFolderSelection(
        state.pendingFolder,
        viewModel::acceptFolder,
        viewModel::dismissFolder,
        page,
    )
    LaunchedEffect(page) {
        if (page == SettingsPage.Notices) viewModel.loadAcknowledgements()
    }
    val back = { openPage(page.parent) }
    BackHandler(page != SettingsPage.Overview, back)
    val actions = SettingsActions(
        openPage = openPage,
        back = back,
        displayMode = viewModel::setDisplayMode,
        language = language.open,
        showStats = viewModel::showStats,
        showPins = viewModel::showPins,
        connect = viewModel::requestFolder,
        replaceBookFolder = viewModel::replaceBookFolder,
        removeBookFolder = viewModel::removeBookFolder,
        sourceApp = viewModel::selectSourceApp, disconnect = {
            confirmation =
                Confirmation.Disconnect
        },
        refresh = viewModel::refresh, clearCache = { confirmation = Confirmation.ClearCache },
        requestLauncher = environment.launcher.request,
        systemSettings = environment.launcher.openSettings,
        reviewSetup = onReviewSetup,
        website = {
            openDeveloperWebsite(context)
        }, retry = viewModel::retryRead, dismiss = viewModel::dismissError,
        retryAcknowledgements = viewModel::loadAcknowledgements,
    )
    key(page) {
        SettingsScreen(
            state = state.copy(language = language.current),
            page = page,
            isDefaultLauncher = environment.isDefaultLauncher,
            actions = actions,
            modifier = modifier,
            contentPadding = contentPadding,
        )
    }
    SettingsConfirmation(
        confirmation,
        viewModel::disconnect,
        viewModel::clearCache,
        onDismiss = { confirmation = null },
    )
}

@Composable
private fun SettingsConfirmation(
    selected: Confirmation?,
    disconnect: () -> Unit,
    clearCache: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (selected == null) return
    val disconnecting = selected == Confirmation.Disconnect
    ConfirmationDialog(
        title = stringResource(
            if (disconnecting) R.string.disconnect_source else R.string.clear_reading_cache,
        ),
        message = stringResource(
            if (disconnecting) {
                R.string.disconnect_description
            } else {
                R.string.clear_cache_description
            },
        ),
        confirmLabel = stringResource(
            if (disconnecting) R.string.confirm_disconnect else R.string.confirm_clear_cache,
        ),
        onConfirm = {
            onDismiss()
            if (disconnecting) disconnect() else clearCache()
        },
        onDismiss = onDismiss,
    )
}

private fun openDeveloperWebsite(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://2techy.dev")))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.website_unavailable, Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.website_unavailable, Toast.LENGTH_SHORT).show()
    }
}
