package dev.alifhasnain.falconer.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationInstrumentedTest {

    @Test
    fun ensureChannel_createsChannelOnApi26Plus() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper(context).ensureChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_ID))
        }
    }
}
