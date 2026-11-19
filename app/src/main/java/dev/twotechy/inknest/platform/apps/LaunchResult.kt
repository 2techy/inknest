package dev.twotechy.inknest.platform.apps

sealed interface LaunchResult {
    data object Success : LaunchResult
    data object Unavailable : LaunchResult
    data class Denied(val message: String) : LaunchResult
}
