package dev.alifhasnain.falconer.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Shares exported text via [Intent.ACTION_SEND], inline or as a file. */
object ShareDelegate {

    fun shareText(context: Context, text: String, subject: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        launch(context, intent)
    }

    fun shareTextFile(context: Context, fileName: String, content: String) {
        val dir = File(context.cacheDir, "falconer_exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.falconer.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launch(context, intent)
    }

    private fun launch(context: Context, intent: Intent) {
        val chooser = Intent.createChooser(intent, "Share").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
