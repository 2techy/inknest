package dev.twotechy.inknest.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

internal fun interface BookIntentLauncher {
    fun launch(intent: Intent): OpenResult
    fun prepare(intent: Intent): Intent = intent
    fun unavailableReason(intent: Intent): OpenResult? = null
}

// Dispatching a VIEW intent does not prove that the external reader opened the document.
internal class AndroidBookIntentLauncher(private val context: Context) : BookIntentLauncher {
    override fun prepare(intent: Intent): Intent = koreaderDocumentIntent(intent) { authority ->
        context.packageManager.resolveContentProvider(authority, 0)?.packageName
    }

    override fun unavailableReason(intent: Intent): OpenResult? {
        val packageName = checkNotNull(intent.`package`)
        val app = try {
            context.packageManager.getApplicationInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return OpenResult.MissingApp
        }
        if (!app.enabled) return OpenResult.MissingApp
        if (context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ==
            null
        ) {
            return OpenResult.Unsupported
        }
        return null
    }
    override fun launch(intent: Intent): OpenResult {
        context.startActivity(intent)
        return OpenResult.Dispatched
    }
}
