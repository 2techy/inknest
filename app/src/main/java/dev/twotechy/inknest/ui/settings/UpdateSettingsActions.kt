package dev.twotechy.inknest.ui.settings

internal data class UpdateSettingsActions(
    val check: () -> Unit = {},
    val automatic: (Boolean) -> Unit = {},
    val download: () -> Unit = {},
    val cancel: () -> Unit = {},
    val install: () -> Unit = {},
    val releasePage: () -> Unit = {},
    val openLink: (String) -> Unit = {},
)
