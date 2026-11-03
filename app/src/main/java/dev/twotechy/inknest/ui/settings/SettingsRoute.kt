package dev.twotechy.inknest.ui.settings

import android.content.ActivityNotFoundException
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
import dev.twotechy.inknest.ui.components.ConfirmationDialog
import dev.twotechy.inknest.ui.components.LauncherActions

private enum class Confirmation { Disconnect, ClearCache }

@Composable
internal fun SettingsRoute(
    viewModel: SettingsViewModel,
    page: SettingsPage,
    openPage: (SettingsPage) -> Unit,
    launcher: LauncherActions,
    isDefaultLauncher: Boolean,
    onReviewSetup: () -> Unit,
    developerTools: @Composable () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmation by remember(page) { mutableStateOf<Confirmation?>(null) }
    var folder by remember(page) { mutableStateOf<SourceFolder?>(null) }
    LaunchedEffect(page) {
        if (page == SettingsPage.Notices) viewModel.loadAcknowledgements()
    }
    val back = {
        openPage(
            if (page == SettingsPage.Notices) {
                SettingsPage.About
            } else {
                SettingsPage.Overview
            },
        )
    }
    BackHandler(page != SettingsPage.Overview, back)
    val actions = SettingsActions(
        openPage = openPage,
        back = back,
        displayMode = viewModel::setDisplayMode,
        showStats = viewModel::showStats,
        showPins = viewModel::showPins,
        connect = { folder = it }, disconnect = { confirmation = Confirmation.Disconnect },
        refresh = viewModel::refresh, clearCache = { confirmation = Confirmation.ClearCache },
        requestLauncher = launcher.request, systemSettings = launcher.openSettings,
        reviewSetup = onReviewSetup,
        website = {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://2techy.dev"),
                    ),
                )
            } catch (
                _: ActivityNotFoundException,
            ) {
                Toast.makeText(
                    context,
                    R.string.website_unavailable,
                    Toast.LENGTH_SHORT,
                ).show()
            } catch (_: SecurityException) {
                Toast.makeText(
                    context,
                    R.string.website_unavailable,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }, retry = viewModel::retryRead, dismiss = viewModel::dismissError,
        retryAcknowledgements = viewModel::loadAcknowledgements,
    )
    key(page) {
        SettingsScreen(
            state = state,
            page = page,
            isDefaultLauncher = isDefaultLauncher,
            actions = actions,
            developerTools = developerTools,
            modifier = modifier,
            contentPadding = contentPadding,
        )
    }
    SourceFolderPicker(
        folder = folder,
        onSelect = { selected ->
            folder = null
            viewModel.connect(selected)
        },
        onDismiss = { folder = null },
    )
    confirmation?.let { selected ->
        val disconnect = selected == Confirmation.Disconnect
        val title = if (disconnect) R.string.disconnect_source else R.string.clear_reading_cache
        val message = if (disconnect) {
            R.string.disconnect_description
        } else {
            R.string.clear_cache_description
        }
        val confirm = if (disconnect) R.string.confirm_disconnect else R.string.confirm_clear_cache
        ConfirmationDialog(
            title = stringResource(title),
            message = stringResource(message),
            confirmLabel = stringResource(confirm),
            onConfirm = {
                confirmation = null
                if (disconnect) viewModel.disconnect() else viewModel.clearCache()
            },
            onDismiss = { confirmation = null },
        )
    }
}
