package dev.twotechy.inknest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dev.twotechy.inknest.ui.InkNestApp
import dev.twotechy.inknest.ui.navigation.AppNavigationViewModel
import java.io.IOException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val navigation by lazy {
        ViewModelProvider(this, AppNavigationViewModel.Factory)[AppNavigationViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigation.handleInitialLaunch(
            isHome = intent.isHomeRequest(),
            restoring = savedInstanceState != null,
            fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0,
        )
        enableEdgeToEdge()
        val container = (application as InkNestApplication).container
        setContent { InkNestApp(container, navigation) }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            try {
                (application as InkNestApplication).container.readingRepository.refresh()
            } catch (_: IOException) {
                // The repository exposes the persisted failure alongside the last cached book.
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.isHomeRequest()) navigation.goHome()
    }
}

private fun Intent.isHomeRequest() =
    action == Intent.ACTION_MAIN && hasCategory(Intent.CATEGORY_HOME)
