package dev.alifhasnain.falconer.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.alifhasnain.falconer.data.HttpTransactionEntity
import dev.alifhasnain.falconer.ui.FalconerActivity

/**
 * Builds and updates the ongoing capture-summary notification: total count plus
 * the most recent transactions (method / path / status). Tapping it opens the
 * inspection UI in its own task; the Clear action wipes captured data.
 *
 * The notification is the **only required UI entry point** for release opt-in
 * users (there is no other launcher), so it shows whenever capture is active.
 */
class NotificationHelper(private val context: Context) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Falconer traffic",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Ongoing summary of captured HTTP traffic"
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /** Shows/updates the summary. No-ops silently if notifications are disabled. */
    fun show(recent: List<HttpTransactionEntity>, total: Int) {
        ensureChannel()

        val lines = recent.take(MAX_LINES).map(::line)
        val style = NotificationCompat.InboxStyle().setBigContentTitle("Falconer · $total captured")
        lines.forEach(style::addLine)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // Platform icon for v1; a custom monochrome icon is future polish.
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Falconer · $total captured")
            .setContentText(lines.firstOrNull() ?: "Capturing…")
            .setStyle(style)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent())
            .addAction(0, "Clear", clearIntent())
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun dismiss() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun openIntent(): PendingIntent {
        val intent = Intent(context, FalconerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, intent, immutableFlags())
    }

    private fun clearIntent(): PendingIntent {
        val intent = Intent(context, FalconerClearReceiver::class.java)
        return PendingIntent.getBroadcast(context, 1, intent, immutableFlags())
    }

    private fun immutableFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    companion object {
        const val CHANNEL_ID = "falconer_traffic"
        const val NOTIFICATION_ID = 0xFA1C

        private const val MAX_LINES = 5

        /** One summary line for a transaction. Pure; unit-tested. */
        fun line(tx: HttpTransactionEntity): String {
            val status = tx.statusCode?.toString() ?: if (tx.error != null) "ERR" else "···"
            val path = tx.path.ifBlank { "/" }
            return "${tx.method} $path  $status"
        }
    }
}
