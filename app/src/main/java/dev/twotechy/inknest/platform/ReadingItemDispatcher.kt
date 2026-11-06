package dev.twotechy.inknest.platform

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import dev.twotechy.inknest.data.repository.ReadingOpenRepository
import dev.twotechy.inknest.model.OpenResult
import dev.twotechy.inknest.model.PreparedReadingOpen
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

// Android URI translation/grants stay in the platform boundary; no book content is read here.
internal class ReadingItemDispatcher(
    context: Context,
    private val repository: ReadingOpenRepository,
    private val launcher: BookIntentLauncher = AndroidBookIntentLauncher(context),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun dispatch(prepared: PreparedReadingOpen): OpenResult = try {
        val intent = withContext(ioDispatcher) { launcher.prepare(prepared.intent()) }
        val unavailable = withContext(ioDispatcher) { launcher.unavailableReason(intent) }
        if (!repository.isCurrent(prepared)) {
            OpenResult.SourceChanged
        } else {
            currentCoroutineContext().ensureActive()
            unavailable ?: withContext(Dispatchers.Main.immediate) { launcher.launch(intent) }
        }
    } catch (_: SecurityException) {
        OpenResult.PermissionLost
    } catch (_: ActivityNotFoundException) {
        OpenResult.Unsupported
    } catch (_: IOException) {
        OpenResult.Unavailable
    }
}

private fun PreparedReadingOpen.intent(): Intent = Intent(Intent.ACTION_VIEW).apply {
    val document = Uri.parse(uri)
    setDataAndType(document, mime)
    setPackage(checkNotNull(source.sourcePackage))
    clipData = ClipData.newRawUri("Book", document)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
}
