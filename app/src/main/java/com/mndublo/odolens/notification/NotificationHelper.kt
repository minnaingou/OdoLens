package com.mndublo.odolens.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mndublo.odolens.MainActivity
import com.mndublo.odolens.data.TimeFormatter
import java.util.Calendar

object NotificationHelper {
    private const val CHANNEL_ID = "parking_expiry_channel"
    private const val CHANNEL_NAME = "Parking Expiration Alerts"
    private const val ALARM_REQ_CODE = 1001
    private const val EXPIRY_REQ_CODE = 1002
    const val EXTRA_OPEN_EXTEND_SHEET = "open_extend_sheet"
    const val EXTRA_OPEN_PARKING_TAB = "open_parking_tab"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies users before their free parking expires"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleParkingAlarm(
        context: Context,
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int,
        parkingSpotNote: String = "",
        use12h: Boolean = false
    ): Long {
        createNotificationChannel(context)

        val alarmTimeMs = calculateAlarmTime(startTime, freeDurationMinutes, offsetMinutes)
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ParkingAlarmReceiver::class.java).apply {
            putExtra("start_time", startTime)
            putExtra("free_duration", freeDurationMinutes)
            putExtra("offset_minutes", offsetMinutes)
            putExtra("parking_spot_note", parkingSpotNote)
            putExtra("use_12h", use12h)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMs,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTimeMs,
                pendingIntent
            )
        }

        return alarmTimeMs
    }

    fun cancelParkingAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ParkingAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        clearNotifications(context)
    }

    fun clearNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(INSTANT_NOTIFICATION_ID)
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
    }

    private fun calculateAlarmTime(startTime: String, freeDurationMinutes: Int, offsetMinutes: Int): Long {
        val parts = startTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Add free duration, then subtract the warning offset. No next-day roll: a past
        // alarm time is scheduled as-is and AlarmManager fires it immediately.
        calendar.add(Calendar.MINUTE, freeDurationMinutes)
        calendar.add(Calendar.MINUTE, -offsetMinutes)
        return calendar.timeInMillis
    }

    fun showInstantConfirmationNotification(
        context: Context,
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int,
        scheduledAlarmTimeStr: String,
        parkingSpotNote: String = "",
        use12h: Boolean = false
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Calculate expiry wall-clock time
        val expirationCalendar = Calendar.getInstance().apply {
            val parts = startTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, freeDurationMinutes)
        }

        val expiryTimeStr = TimeFormatter.formatTime(
            expirationCalendar.get(Calendar.HOUR_OF_DAY),
            expirationCalendar.get(Calendar.MINUTE),
            use12h
        )
        val noteText = if (parkingSpotNote.isNotBlank()) " 📍 Spot: $parkingSpotNote" else ""
        val title = "Parking Timer Active$noteText"
        val content = "Expires at $expiryTimeStr (Alert set for $scheduledAlarmTimeStr)"

        // Build action PendingIntents
        // Extend: opens MainActivity → Parking tab → extend sheet
        val extendActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_EXTEND_SHEET, true)
        }
        val extendPi = PendingIntent.getActivity(
            context, REQ_EXTEND_1H, extendActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = Intent(ParkingExtendReceiver.ACTION_CANCEL).apply {
            setClass(context, ParkingExtendReceiver::class.java)
        }
        val cancelPi = PendingIntent.getBroadcast(
            context, REQ_CANCEL, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build content PendingIntent for tapping notification body
        val contentActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_PARKING_TAB, true)
        }
        val contentPi = PendingIntent.getActivity(
            context, REQ_CONTENT_PARKING, contentActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setContentIntent(contentPi)
            .addAction(android.R.drawable.ic_media_ff, "Extend Hours", extendPi)
            .addAction(android.R.drawable.ic_delete, "\u2715 Cancel", cancelPi)
            .build()

        notificationManager.notify(INSTANT_NOTIFICATION_ID, notification)
    }

    fun showNotification(
        context: Context,
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int,
        parkingSpotNote: String = "",
        use12h: Boolean = false
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Calculate expiry wall-clock time
        val expirationCalendar = Calendar.getInstance().apply {
            val parts = startTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, freeDurationMinutes)
        }
        
        val expiryTimeStr = TimeFormatter.formatTime(
            expirationCalendar.get(Calendar.HOUR_OF_DAY),
            expirationCalendar.get(Calendar.MINUTE),
            use12h
        )
        val startTimeFormatted = TimeFormatter.formatStartTime(startTime, use12h)
        val noteText = if (parkingSpotNote.isNotBlank()) " [Spot: $parkingSpotNote]" else ""
        val title = "Parking Expiry Alert!$noteText"
        val content = "Free parking started at $startTimeFormatted expires at $expiryTimeStr. Only $offsetMinutes min remaining!"

        // Extend: opens MainActivity → Parking tab → extend sheet
        val extendActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_EXTEND_SHEET, true)
        }
        val extendPi = PendingIntent.getActivity(
            context, REQ_EXTEND_1H, extendActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = Intent(ParkingExtendReceiver.ACTION_CANCEL).apply {
            setClass(context, ParkingExtendReceiver::class.java)
        }
        val cancelPi = PendingIntent.getBroadcast(
            context, REQ_CANCEL, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build content PendingIntent for tapping notification body
        val contentActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_PARKING_TAB, true)
        }
        val contentPi = PendingIntent.getActivity(
            context, REQ_CONTENT_PARKING, contentActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setContentIntent(contentPi)
            .addAction(android.R.drawable.ic_media_ff, "Extend Hours", extendPi)
            .addAction(android.R.drawable.ic_delete, "\u2715 Cancel", cancelPi)
            .build()

        notificationManager.notify(ALARM_NOTIFICATION_ID, notification)
    }

    private const val INSTANT_NOTIFICATION_ID = 2
    private const val ALARM_NOTIFICATION_ID = 1
    const val EXPIRY_NOTIFICATION_ID = 3
    private const val REQ_EXTEND_1H = 2001
    private const val REQ_CANCEL = 2002
    private const val REQ_CONTENT_PARKING = 2003

    // -------------------------------------------------------------------------
    // Expiry alarm: fires at the exact moment parking expires
    // -------------------------------------------------------------------------

    fun scheduleExpiryAlarm(
        context: Context,
        expiryMs: Long,
        parkingSpotNote: String = "",
        use12h: Boolean = false
    ) {
        if (expiryMs <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ParkingExpiredReceiver::class.java).apply {
            putExtra("parking_spot_note", parkingSpotNote)
            putExtra("use_12h", use12h)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, EXPIRY_REQ_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiryMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, expiryMs, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiryMs, pendingIntent)
        }
    }

    fun cancelExpiryAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ParkingExpiredReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, EXPIRY_REQ_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showExpiredNotification(context: Context, parkingSpotNote: String = "") {
        createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cancel the ongoing "Parking Timer Active" and pre-expiry notifications
        notificationManager.cancel(INSTANT_NOTIFICATION_ID)
        notificationManager.cancel(ALARM_NOTIFICATION_ID)

        val noteText = if (parkingSpotNote.isNotBlank()) " — Spot: $parkingSpotNote" else ""

        // Tap notification → open Parking tab
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_PARKING_TAB, true)
        }
        val contentPi = PendingIntent.getActivity(
            context, REQ_CONTENT_PARKING, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action: clears the expired timer state directly without opening the app
        val dismissIntent = Intent(ParkingExtendReceiver.ACTION_CANCEL).apply {
            setClass(context, ParkingExtendReceiver::class.java)
        }
        val dismissPi = PendingIntent.getBroadcast(
            context, REQ_CANCEL, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⏰ Parking Expired$noteText")
            .setContentText("Your free parking time has ended.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setDeleteIntent(dismissPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Clear Parking", dismissPi)
            .build()

        notificationManager.notify(EXPIRY_NOTIFICATION_ID, notification)
    }
}
