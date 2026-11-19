package dev.twotechy.inknest.platform.reading

import android.content.Intent
import dev.twotechy.inknest.model.OpenResult

internal fun interface BookIntentLauncher {
    fun launch(intent: Intent): OpenResult
    fun prepare(intent: Intent): Intent = intent
    fun unavailableReason(intent: Intent): OpenResult? = null
}

// Dispatching a VIEW intent does not prove that the external reader opened the document.
