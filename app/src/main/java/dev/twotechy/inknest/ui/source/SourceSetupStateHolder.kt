package dev.twotechy.inknest.ui.source

import androidx.lifecycle.SavedStateHandle
import dev.twotechy.inknest.data.repository.BookFolderConflict
import dev.twotechy.inknest.data.repository.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.repository.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.SourceSettingsState
import dev.twotechy.inknest.data.repository.SourceTreeSelection
import dev.twotechy.inknest.model.AppInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal enum class SourceSetupError { Source, FolderConflict }
internal data class SourceSetupUiState(
    val source: SourceSettingsState = SourceSettingsState(),
    val apps: List<AppInfo> = emptyList(),
    val pendingFolder: SourceFolder? = null,
    val saving: Boolean = false,
    val error: SourceSetupError? = null,
)

// Each screen owns its actions/picker. Only persisted source data is shared by repositories.
internal class SourceSetupStateHolder(
    private val repository: SourceSettingsRepository,
    apps: InstalledAppsRepository,
    private val saved: SavedStateHandle,
    private val scope: CoroutineScope,
) {
    private data class Operation(val saving: Boolean = false, val error: SourceSetupError? = null)
    private val operation = MutableStateFlow(Operation())
    private val pending = saved.getStateFlow<SourceFolder?>("source.pendingFolder", null)
    val state = combine(repository.state, apps.installedApps, pending, operation) {
            source,
            installed,
            folder,
            action,
        ->
        SourceSetupUiState(
            source,
            installed.filter { it.componentId.substringBefore('/') == "org.koreader.launcher" }
                .distinctBy { it.componentId.substringBefore('/') },
            folder,
            action.saving,
            action.error,
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), SourceSetupUiState())

    fun requestFolder(folder: SourceFolder) {
        saved["source.replacedTree"] = null
        saved["source.pendingFolder"] = folder
    }
    fun replaceBookFolder(uri: String) {
        saved["source.replacedTree"] = uri
        saved["source.pendingFolder"] = SourceFolder.Books
    }
    fun acceptFolder(folder: SourceFolder, uri: String, flags: Int) {
        val previous = saved.get<String>("source.replacedTree")
        dismissFolder()
        perform { repository.connectTree(SourceTreeSelection(folder, uri, flags, previous)) }
    }
    fun dismissFolder() {
        saved["source.pendingFolder"] = null
        saved["source.replacedTree"] = null
    }
    fun selectApp(packageName: String) = perform { repository.selectApp(packageName) }
    fun removeBookFolder(uri: String) = perform { repository.removeBookTree(uri) }
    fun disconnect() = perform { repository.disconnect() }
    fun refresh() = perform { repository.refresh() }
    fun clearCache() = perform { repository.clearCache() }
    fun dismissError() {
        operation.value = operation.value.copy(error = null)
    }

    private fun perform(block: suspend () -> Unit) {
        if (operation.value.saving) return
        operation.value = Operation(saving = true)
        scope.launch {
            try {
                block()
                operation.value = Operation()
            } catch (cancelled: CancellationException) {
                operation.value = Operation()
                throw cancelled
            } catch (error: Exception) {
                operation.value = Operation(
                    error = if (error is BookFolderConflict) {
                        SourceSetupError.FolderConflict
                    } else {
                        SourceSetupError.Source
                    },
                )
            }
        }
    }
}
