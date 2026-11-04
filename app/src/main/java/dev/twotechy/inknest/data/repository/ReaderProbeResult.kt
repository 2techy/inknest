package dev.twotechy.inknest.data.repository

import dev.twotechy.inknest.data.source.ResolvedBook
import dev.twotechy.inknest.data.source.epub.EpubProbeResult
import dev.twotechy.inknest.data.source.lua.HistoryEntryDto
import dev.twotechy.inknest.data.source.lua.SidecarDto
import dev.twotechy.inknest.data.source.stats.StatisticsProbeResult

internal enum class ProbeIssue {
    NotConfigured,
    MissingFile,
    PermissionLost,
    InvalidLua,
    InvalidMetadata,
    InvalidSnapshot,
    SourceChanged,
    TooLarge,
    BudgetExceeded,
    ProviderError,
    UnresolvedPath,
    Unsupported,
    UnsupportedSchema,
}
internal sealed interface ProbeRead<out T> {
    data class Success<T>(val value: T) : ProbeRead<T>
    data class Unavailable(val issue: ProbeIssue) : ProbeRead<Nothing>
    data class Failed(val issue: ProbeIssue) : ProbeRead<Nothing>
    data object NotTested : ProbeRead<Nothing>
}
internal data class CapabilityResult<out T>(val result: ProbeRead<T>, val checkedAt: Long)
internal data class ReaderProbeResult(
    val current: CapabilityResult<String?>,
    val history: CapabilityResult<List<HistoryEntryDto>>,
    val resolvedCurrent: CapabilityResult<ResolvedBook?> = CapabilityResult(ProbeRead.NotTested, 0),
    val resolvedHistory: List<CapabilityResult<ResolvedBook>> = emptyList(),
    val sidecar: CapabilityResult<SidecarDto?> = CapabilityResult(ProbeRead.NotTested, 0),
    val epub: CapabilityResult<EpubProbeResult?> = CapabilityResult(ProbeRead.NotTested, 0),
    val statistics: CapabilityResult<StatisticsProbeResult> =
        CapabilityResult(ProbeRead.NotTested, 0),
)
