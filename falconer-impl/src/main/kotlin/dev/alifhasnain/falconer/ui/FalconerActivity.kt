package dev.alifhasnain.falconer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dev.alifhasnain.falconer.ui.theme.FalconerTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * The inspection UI host. Declared in its own task (singleTask + taskAffinity)
 * so it lives in a separate recents entry from the host app (Chucker-style).
 */
class FalconerActivity : ComponentActivity() {

    // CONFLATED so a re-launch is kept until the composition collects it.
    private val reopened = Channel<Unit>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light theme -> dark status/nav-bar icons, or they vanish on white chrome.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        val reopenedFlow = reopened.receiveAsFlow()
        setContent {
            FalconerTheme {
                FalconerApp(reopened = reopenedFlow)
            }
        }
    }

    // singleTask: a notification tap or launchUi while the UI is alive lands here,
    // not in onCreate. FalconerApp answers it by returning to the top of the list.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        reopened.trySend(Unit)
    }

    companion object {
        /** Launches the inspection UI from any context (e.g. the plugin). */
        fun start(context: Context) {
            val intent = Intent(context, FalconerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
