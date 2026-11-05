package dev.twotechy.inknest.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.ReaderProbeRepository
import dev.twotechy.inknest.data.repository.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.SourceFolder
import dev.twotechy.inknest.data.repository.SourceSettingsRepository
import dev.twotechy.inknest.data.source.SafSourceAccess
import dev.twotechy.inknest.model.ReadingSourceConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SourceProbeViewModel(
    private val repository: ReadingSourceConfigRepository,
    private val access: SafSourceAccess,
    private val savedState: SavedStateHandle,
    private val readerRepository: ReaderProbeRepository,
    private val sourceSettings: SourceSettingsRepository,
) : ViewModel() {
    private val current = MutableStateFlow(SourceProbeUiState())
    val uiState = current.asStateFlow()
    private var probeJob: Job? = null
    private var probeGeneration = 0L
    val visible = savedState.getStateFlow("probeVisible", false)
    val pending = savedState.getStateFlow<SourceFolder?>("probePendingFolder", null)
    fun open() {
        savedState["probeVisible"] = true
    }
    fun close() {
        cancelProbe()
        dismissFolder()
        savedState["probeVisible"] = false
    }
    fun chooseFolder(folder: SourceFolder) {
        savedState["probePendingFolder"] = folder
    }
    fun dismissFolder() {
        savedState["probePendingFolder"] = null
    }

    init {
        viewModelScope.launch {
            try {
                repository.config.collect { config ->
                    cancelProbe()
                    current.update { it.copy(config = config, reader = null) }
                    inspect(config)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                current.value = current.value.copy(failed = true)
            }
        }
    }

    private suspend fun inspect(config: ReadingSourceConfig) {
        val koreader = access.inspect(config.koreaderTreeUri)
        val books = access.inspect(config.booksTreeUri)
        current.update {
            if (it.config == config) it.copy(koreader = koreader, books = books) else it
        }
    }

    fun recheck() = accessAction { inspect(current.value.config) }

    fun runProbe() {
        if (current.value.probing || current.value.busy) return
        val generation = ++probeGeneration
        val config = current.value.config
        current.update { it.copy(probing = true, reader = null, failed = false) }
        probeJob = viewModelScope.launch {
            try {
                val result = readerRepository.probe(config)
                current.update {
                    if (generation == probeGeneration &&
                        it.config == config
                    ) {
                        it.copy(reader = result)
                    } else {
                        it
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                current.update { it.copy(failed = true) }
            } finally {
                if (generation == probeGeneration) current.update { it.copy(probing = false) }
            }
        }
    }

    fun cancelProbe() {
        probeGeneration++
        probeJob?.cancel()
        probeJob = null
        current.update { it.copy(probing = false) }
    }

    fun select(folder: SourceFolder, uri: String, flags: Int) {
        dismissFolder()
        accessAction {
            cancelProbe()
            sourceSettings.connectTree(folder, uri, flags)
        }
    }

    fun selectPackage(packageName: String) = accessAction {
        repository.setSourcePackage(packageName)
    }

    private fun accessAction(block: suspend () -> Unit) {
        if (current.value.busy) return
        current.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                current.update { it.copy(failed = true) }
            } finally {
                current.update { it.copy(busy = false) }
            }
        }
    }
}
