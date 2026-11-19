package dev.twotechy.inknest.data.source.probe

internal sealed interface ProbeRead<out T> {
    data class Success<T>(val value: T) : ProbeRead<T>
    data class Unavailable(val issue: ProbeIssue) : ProbeRead<Nothing>
    data class Failed(val issue: ProbeIssue) : ProbeRead<Nothing>
}
