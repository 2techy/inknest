package dev.twotechy.inknest.model

enum class OpenResult {
    Dispatched,
    Unsupported,
    SourceChanged,
    MissingApp,
    MissingFile,
    PermissionLost,
    Unavailable,
}
