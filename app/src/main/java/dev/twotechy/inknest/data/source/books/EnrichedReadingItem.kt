package dev.twotechy.inknest.data.source.books

import dev.twotechy.inknest.model.ReadingItem

internal data class EnrichedReadingItem(val item: ReadingItem, val metadata: BookMetadata?)
