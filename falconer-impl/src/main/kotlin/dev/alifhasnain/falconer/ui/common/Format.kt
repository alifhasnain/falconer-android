package dev.alifhasnain.falconer.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Display formatting helpers — unambiguous, plain (PRODUCT brand voice). */
object Format {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun time(epochMs: Long): String = timeFormat.format(Date(epochMs))

    fun duration(tookMs: Long?): String = when {
        tookMs == null -> "—"
        tookMs < 1000 -> "$tookMs ms"
        else -> String.format(Locale.US, "%.2f s", tookMs / 1000.0)
    }

    fun size(bytes: Long?): String = when {
        bytes == null -> "—"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
