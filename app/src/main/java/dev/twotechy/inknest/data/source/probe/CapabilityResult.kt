package dev.twotechy.inknest.data.source.probe

internal data class CapabilityResult<out T>(val result: ProbeRead<T>, val checkedAt: Long)
