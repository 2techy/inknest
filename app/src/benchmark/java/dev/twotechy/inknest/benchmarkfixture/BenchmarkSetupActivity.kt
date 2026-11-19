package dev.twotechy.inknest.benchmarkfixture

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import dev.twotechy.inknest.data.local.preferences.userPreferencesDataStore
import dev.twotechy.inknest.data.repository.preferences.impl.DataStoreUserPreferencesRepository
import dev.twotechy.inknest.data.repository.sources.ReadingSourceConfigRepository
import dev.twotechy.inknest.data.repository.sources.SourceFolder
import dev.twotechy.inknest.data.source.apps.impl.AndroidInstalledAppsDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Exported only in performance variants and protected by the shell's DUMP permission.
class BenchmarkSetupActivity : ComponentActivity() {
    private val dataset = BenchmarkDataset()
    private var preparation: Job? = null
    private val picker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null) {
                preparation = lifecycleScope.launch { configure(uri) }
            } else {
                status("Benchmark folder selection cancelled")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepare()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        prepare()
    }

    private fun prepare() {
        val previous = preparation
        previous?.cancel()
        preparation = lifecycleScope.launch {
            previous?.join()
            status("Preparing benchmark")
            check(packageName.endsWith(".benchmark"))
            if (Build.VERSION.SDK_INT >= 30) check(Environment.isExternalStorageManager())
            withContext(Dispatchers.IO) { dataset.ensure() }
            val operation = intent.getStringExtra("operation") ?: "prepare"
            withContext(Dispatchers.IO) { dataset.update(changed = operation == "mutate") }
            if (operation != "prepare") {
                check(operation == "reset" || operation == "mutate")
                ready()
                return@launch
            }
            val tree = DocumentsContract.buildTreeDocumentUri(AUTHORITY, dataset.documentId)
            val granted = contentResolver.persistedUriPermissions.any {
                it.uri == tree &&
                    it.isReadPermission
            }
            if (granted) configure(tree) else selectFolder()
        }
    }

    private fun selectFolder() {
        picker.launch(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                )
                putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    DocumentsContract.buildDocumentUri(AUTHORITY, dataset.documentId),
                )
            },
        )
    }

    private suspend fun configure(uri: Uri) {
        check(DocumentsContract.getTreeDocumentId(uri) == dataset.documentId)
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val preferences = DataStoreUserPreferencesRepository(userPreferencesDataStore)
        val config = ReadingSourceConfigRepository(userPreferencesDataStore)
        val before = config.config.first()
        val candidate = config.treeCandidate(before, SourceFolder.KOReader, uri.toString())
            .copy(booksTreeUris = setOf(uri.toString()))
        config.activateTree(before, candidate)
        config.setSourcePackage("org.koreader.launcher")
        preferences.completeOnboarding()
        AndroidInstalledAppsDataSource(applicationContext).loadInstalledApps().take(4).forEach {
            preferences.pin(it.componentId)
        }
        ready()
    }

    private fun ready() = status("Benchmark ready ${intent.getStringExtra("request")}")

    private fun status(message: String) {
        setContentView(
            TextView(this).apply {
                text = message
                textSize = 18f
            },
        )
    }

    private companion object {
        const val AUTHORITY = "com.android.externalstorage.documents"
    }
}
