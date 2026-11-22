package dev.twotechy.inknest.ui.shared

import androidx.lifecycle.SavedStateHandle
import dev.twotechy.inknest.data.repository.apps.InstalledAppsRepository
import dev.twotechy.inknest.data.repository.sources.BookFolderConflictException
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.repository.sources.SourceSettingsRepository
import dev.twotechy.inknest.data.repository.sources.SourceTreeSelection
import dev.twotechy.inknest.model.KOREADER_PACKAGE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class SourceSetupStateHolder(
    private val repository: SourceSettingsRepository,
    apps: InstalledAppsRepository,
    private val saved: SavedStateHandle,
    private val scope: CoroutineScope,
) {
    private data class Operation(
        val saving: Boolean = false,
        val error: SourceSetupError? = null,
        val bookFolderSelection: Boolean = false,
    )
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
            installed.filter { it.componentId.substringBefore('/') == KOREADER_PACKAGE }
                .distinctBy { it.componentId.substringBefore('/') },
            folder,
            action.saving,
            action.error,
            action.bookFolderSelection,
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), SourceSetupUiState())

    fun requestFolder(folder: SourceFolder) {
        clearBookFolderError()
        saved["source.replacedTree"] = null
        saved["source.pendingFolder"] = folder
    }
    fun replaceBookFolder(uri: String) {
        clearBookFolderError()
        saved["source.replacedTree"] = uri
        saved["source.pendingFolder"] = SourceFolder.Books
    }
    fun acceptFolder(folder: SourceFolder, uri: String, flags: Int, pairKoreader: Boolean = false) {
        val previous = saved.get<String>("source.replacedTree")
        dismissFolder()
        perform(bookFolderSelection = folder == SourceFolder.Books) {
            repository.connectTree(SourceTreeSelection(folder, uri, flags, previous, pairKoreader))
        }
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
        val current = operation.value
        if (current.error != null) operation.value = Operation()
    }

    private fun clearBookFolderError() {
        if (operation.value.bookFolderSelection) dismissError()
    }

    private fun perform(bookFolderSelection: Boolean = false, block: suspend () -> Unit) {
        if (operation.value.saving) return
        operation.value = Operation(
            saving = true,
            bookFolderSelection = bookFolderSelection,
        )
        scope.launch {
            try {
                block()
                operation.value = Operation()
            } catch (cancelled: CancellationException) {
                operation.value = Operation()
                throw cancelled
            } catch (error: Exception) {
                operation.value = Operation(
                    error = if (error is BookFolderConflictException) {
                        SourceSetupError.FolderConflict
                    } else {
                        SourceSetupError.Source
                    },
                    bookFolderSelection = bookFolderSelection,
                )
            }
        }
    }
}
