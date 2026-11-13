package dev.twotechy.inknest.ui.settings

import dev.twotechy.inknest.R
import dev.twotechy.inknest.model.AppUpdateState
import dev.twotechy.inknest.model.UpdateDownloadStage

internal enum class UpdatePrimaryAction(val label: Int, val enabled: Boolean = true) {
    Check(R.string.check_for_updates),
    Download(R.string.updates_download),
    Cancel(R.string.cancel),
    Install(R.string.updates_install),
    Checking(R.string.updates_checking, false),
    Preparing(R.string.updates_verifying, false),
}

internal fun updatePrimaryAction(state: AppUpdateState): UpdatePrimaryAction = when {
    state.checking -> UpdatePrimaryAction.Checking
    state.download == UpdateDownloadStage.Downloading -> UpdatePrimaryAction.Cancel
    state.download == UpdateDownloadStage.Verifying -> UpdatePrimaryAction.Preparing
    state.download == UpdateDownloadStage.Ready -> UpdatePrimaryAction.Install
    state.canDownload -> UpdatePrimaryAction.Download
    else -> UpdatePrimaryAction.Check
}

internal fun UpdateSettingsActions.perform(action: UpdatePrimaryAction) {
    when (action) {
        UpdatePrimaryAction.Check -> check()
        UpdatePrimaryAction.Download -> download()
        UpdatePrimaryAction.Cancel -> cancel()
        UpdatePrimaryAction.Install -> install()
        UpdatePrimaryAction.Checking, UpdatePrimaryAction.Preparing -> Unit
    }
}
