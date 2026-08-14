package com.mndublo.odolens.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import com.mndublo.odolens.MainActivity
import com.mndublo.odolens.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ParkingExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val spotNote = intent.getStringExtra("parking_spot_note") ?: ""
        val use12h = intent.getBooleanExtra("use_12h", false)

        // Show "Parking Expired" notification
        NotificationHelper.showExpiredNotification(context, spotNote)

        // Mark the timer state as expired in DataStore
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(context.applicationContext)
                repo.setParkingExpired(true)
            } finally {
                pending.finish()
            }
        }
    }
}
