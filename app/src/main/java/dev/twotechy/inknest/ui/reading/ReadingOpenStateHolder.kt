package dev.twotechy.inknest.ui.reading

import dev.twotechy.inknest.data.repository.ReadingOpenRepository
import dev.twotechy.inknest.model.OpenResult
import dev.twotechy.inknest.model.PreparedReadingOpen
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.model.ReadingOpenPreparation
import dev.twotechy.inknest.model.ReadingOpenRequest
import dev.twotechy.inknest.model.ReadingOpenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ReadingOpenActions(
    val open: (ReadingItemId) -> Unit,
    val claim: (Long) -> PreparedReadingOpen?,
    val complete: (Long, OpenResult) -> Unit,
    val cancel: () -> Unit,
    val dismiss: () -> Unit,
)

internal class ReadingOpenStateHolder(
    private val repository: ReadingOpenRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<ReadingOpenState>(ReadingOpenState.Idle)
    val state = mutableState.asStateFlow()
    private var generation = 0L
    private var job: Job? = null
    val actions = ReadingOpenActions(::open, ::claim, ::complete, ::cancel, ::dismiss)

    private fun open(item: ReadingItemId) {
        if (state.value !is ReadingOpenState.Idle && state.value !is ReadingOpenState.Error) return
        val id = ++generation
        mutableState.value = ReadingOpenState.Preparing
        job = scope.launch {
            val prepared = repository.prepare(item)
            if (generation == id) mutableState.value = prepared.state(id)
        }
    }
    private fun claim(id: Long): PreparedReadingOpen? {
        val ready = state.value as? ReadingOpenState.Ready ?: return null
        if (ready.request.id != id) return null
        mutableState.value = ReadingOpenState.Dispatching(ready.request)
        return ready.request.prepared
    }
    private fun complete(id: Long, result: OpenResult) {
        val dispatching = state.value as? ReadingOpenState.Dispatching ?: return
        if (dispatching.request.id != id) return
        mutableState.value = if (result == OpenResult.Dispatched) {
            ReadingOpenState.Idle
        } else {
            ReadingOpenState.Error(result)
        }
    }
    private fun cancel() {
        generation++
        job?.cancel()
        mutableState.value = ReadingOpenState.Idle
    }
    private fun dismiss() {
        if (state.value is ReadingOpenState.Error) mutableState.value = ReadingOpenState.Idle
    }
}

private fun ReadingOpenPreparation.state(id: Long): ReadingOpenState = when (this) {
    is ReadingOpenPreparation.Ready -> ReadingOpenState.Ready(ReadingOpenRequest(id, prepared))
    is ReadingOpenPreparation.Failed -> ReadingOpenState.Error(error)
}
