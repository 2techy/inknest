package dev.twotechy.inknest.data.source.probe

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
