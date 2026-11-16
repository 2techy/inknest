package dev.twotechy.inknest.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twotechy.inknest.data.repository.BookDetailsRepository
import dev.twotechy.inknest.model.BookDetails
import dev.twotechy.inknest.model.BookStatisticsStatus
import dev.twotechy.inknest.model.ReadingItemId
import dev.twotechy.inknest.platform.clock.StatisticsClock
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn

internal data class BookDetailsUiState(
    val details: BookDetails = BookDetails(),
    val zone: ZoneId = ZoneId.systemDefault(),
    val descriptionExpanded: Boolean = false,
    val historyExpanded: Boolean = false,
    val fileExpanded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal class BookDetailsViewModel(
    private val repository: BookDetailsRepository,
    private val savedState: SavedStateHandle,
    clock: StatisticsClock,
) : ViewModel() {
    private val identity = savedState.getStateFlow<ArrayList<String>?>("detailsIdentity", null)
    private val limit = savedState.getStateFlow("detailsHistoryLimit", 30)
    private val data = combine(identity, limit) { parts, count -> parts to count }
        .flatMapLatest { (parts, count) ->
            val id = parts?.takeIf { it.size == 2 }?.let { ReadingItemId(it[0], it[1]) }
            if (id == null) flowOf(BookDetails()) else repository.observe(id, count)
        }.runningFold(BookDetails()) { previous, current ->
            if (current.book != null && current.book.id == previous.book?.id &&
                current.statistics.status == BookStatisticsStatus.Loading &&
                previous.statistics.status == BookStatisticsStatus.Ready
            ) {
                current.copy(statistics = previous.statistics)
            } else {
                current
            }
        }
    val uiState = combine(
        combine(data, clock.moments) { data, moment -> data to moment.zone },
        savedState.getStateFlow("detailsDescription", false),
        savedState.getStateFlow("detailsHistory", false),
        savedState.getStateFlow("detailsFile", false),
    ) { details, description, history, file ->
        BookDetailsUiState(details.first, details.second, description, history, file)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookDetailsUiState())

    fun select(id: ReadingItemId?) {
        val value = id?.let { arrayListOf(it.sourceId, it.path) }
        if (savedState.get<ArrayList<String>>("detailsIdentity") == value) return
        savedState["detailsIdentity"] = value
        savedState["detailsHistoryLimit"] = 30
        listOf("detailsDescription", "detailsHistory", "detailsFile").forEach {
            savedState[it] =
                false
        }
    }
    fun toggleDescription() = toggle("detailsDescription")
    fun toggleHistory() = toggle("detailsHistory")
    fun toggleFile() = toggle("detailsFile")
    private fun toggle(key: String) {
        savedState[key] = savedState.get<Boolean>(key) != true
    }
    fun moreHistory() {
        savedState["detailsHistoryLimit"] = (limit.value + 30).coerceAtMost(1_000_000)
    }
    fun retry() = repository.retry()
}
