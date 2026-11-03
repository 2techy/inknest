package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.source.lua.HistoryEntryDto

internal enum class ProbeIssue {
    NotConfigured,
    MissingFile,
    PermissionLost,
    InvalidLua,
    TooLarge,
    BudgetExceeded,
    ProviderError,
}
internal sealed interface ProbeRead<out T> {
    data class Success<T>(val value: T) : ProbeRead<T>
    data class Failed(val issue: ProbeIssue) : ProbeRead<Nothing>
    data object NotTested : ProbeRead<Nothing>
}
internal data class CapabilityResult<T>(val result: ProbeRead<T>, val checkedAt: Long)
internal data class ReaderProbeResult(
    val current: CapabilityResult<String?>,
    val history: CapabilityResult<List<HistoryEntryDto>>,
)
