package dev.alifhasnain.falconer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dev.alifhasnain.falconer.ui.theme.FalconerTheme

/**
 * The inspection UI host. Declared in its own task (singleTask + taskAffinity)
 * so it lives in a separate recents entry from the host app (Chucker-style).
 */
class FalconerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light theme -> dark status/nav-bar icons, or they vanish on white chrome.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            FalconerTheme {
                FalconerApp()
            }
        }
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
