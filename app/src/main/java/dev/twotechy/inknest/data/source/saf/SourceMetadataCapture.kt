package dev.twotechy.inknest.data.source.saf

import dev.twotechy.inknest.data.source.saf.SourceMetadataFingerprint.Document
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class SourceMetadataCapture(private val startedAt: Long) :
    AbstractCoroutineContextElement(Key) {
    private val documents = linkedMapOf<Document, SourceDocument?>()
    private var failed = false

    @Synchronized fun record(reference: Document, document: SourceDocument?) {
        if (failed) return
        if (reference !in documents && documents.size >= MAX_DOCUMENTS) {
            fail()
            return
        }
        if (reference in documents && documents[reference] != document) {
            fail()
            return
        }
        documents[reference] = document
    }

    @Synchronized fun fail() {
        failed = true
        documents.clear()
    }

    @Synchronized fun fingerprint(): SourceMetadataFingerprint? {
        if (failed) return null
        return SourceMetadataFingerprint(documents.toMap()).takeIf { it.reusableAt(startedAt) }
    }

    companion object Key : CoroutineContext.Key<SourceMetadataCapture> {
        private const val MAX_DOCUMENTS = 4096
    }
}
