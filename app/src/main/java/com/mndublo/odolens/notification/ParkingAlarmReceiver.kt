package com.mndublo.odolens.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ParkingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val startTime = intent.getStringExtra("start_time") ?: "00:00"
        val freeDuration = intent.getIntExtra("free_duration", 0)
        val offsetMinutes = intent.getIntExtra("offset_minutes", 60)
        val parkingSpotNote = intent.getStringExtra("parking_spot_note") ?: ""
        val use12h = intent.getBooleanExtra("use_12h", false)

        NotificationHelper.showNotification(context, startTime, freeDuration, offsetMinutes, parkingSpotNote, use12h)
    }
}
