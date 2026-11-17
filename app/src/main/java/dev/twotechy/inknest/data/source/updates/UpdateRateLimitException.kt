package dev.twotechy.inknest.data.source.updates

import java.io.IOException

internal class UpdateRateLimitException(val retryAt: Long) : IOException("GitHub rate limit")
