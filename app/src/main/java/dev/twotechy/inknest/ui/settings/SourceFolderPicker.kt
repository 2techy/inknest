package dev.twotechy.inknest.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.twotechy.inknest.data.repository.SourceFolder

@Composable
internal fun SourceFolderPicker(
    folder: SourceFolder?,
    onSelect: (SourceFolder, String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var launched by rememberSaveable { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val uri = result.data?.data
            if (result.resultCode == Activity.RESULT_OK && uri != null && folder != null) {
                onSelect(folder, uri.toString(), result.data?.flags ?: 0)
            } else {
                onDismiss()
            }
        }
    LaunchedEffect(folder) {
        if (folder == null) {
            launched = false
            return@LaunchedEffect
        }
        if (!launched) {
            launched = true
            picker.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                    )
                },
            )
        }
    }
}
