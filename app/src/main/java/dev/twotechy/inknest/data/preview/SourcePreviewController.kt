package dev.twotechy.inknest.data.preview

import dev.twotechy.inknest.data.repository.SourceStatus

interface SourcePreviewController {
    fun selectStatus(status: SourceStatus)
    fun failNextAction()
}
