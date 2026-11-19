package dev.twotechy.inknest.model

enum class SourceAccessState {
    NotSelected,
    Granted,
    PermissionLost,
    InvalidFolder,
    Unsupported,
    ProviderError,
}
