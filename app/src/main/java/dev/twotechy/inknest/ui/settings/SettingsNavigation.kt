package dev.twotechy.inknest.ui.settings

internal data class SettingsNavigation(
    val page: SettingsPage,
    val openPage: (SettingsPage) -> Unit,
    val exit: () -> Unit,
) {
    fun back() {
        if (page == SettingsPage.Overview) exit() else openPage(page.parent)
    }
}
