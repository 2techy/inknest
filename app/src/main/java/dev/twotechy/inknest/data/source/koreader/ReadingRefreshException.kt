package dev.twotechy.inknest.data.source.koreader

import dev.twotechy.inknest.model.ReadingSyncIssue
import java.io.IOException

internal class ReadingRefreshException(val issue: ReadingSyncIssue, cause: Throwable? = null) :
    IOException(issue.name, cause)
