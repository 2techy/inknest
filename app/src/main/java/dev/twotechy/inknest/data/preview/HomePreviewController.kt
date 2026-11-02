package dev.twotechy.inknest.data.preview

internal enum class HomePreviewMode { Content, Empty, Loading, Error }

// Optional developer control. Only the debug source set provides an implementation.
internal interface HomePreviewController {
    fun select(mode: HomePreviewMode)
}
