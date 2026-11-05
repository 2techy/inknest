package dev.twotechy.inknest.data.repository

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
}
internal data class CapabilityResult<out T>(val result: ProbeRead<T>, val checkedAt: Long)
