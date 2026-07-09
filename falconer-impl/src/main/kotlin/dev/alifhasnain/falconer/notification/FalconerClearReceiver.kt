package dev.alifhasnain.falconer.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.alifhasnain.falconer.data.FalconerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the notification "Clear" action: wipes captured data and dismisses. */
class FalconerClearReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FalconerDatabase.get(appContext).transactionDao().clear()
            } finally {
                NotificationHelper(appContext).dismiss()
                pending.finish()
            }
        }
    }
}
