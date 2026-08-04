package com.mndublo.odolens.ui.parking

import android.content.Context
import com.mndublo.odolens.data.ParkingSettingsSource
import com.mndublo.odolens.notification.NotificationHelper
import com.mndublo.odolens.notification.ParkingTimerManager

/**
 * OS-alarm + notification operations for the parking feature.
 * Abstracted so [ParkingViewModel] can be unit-tested without Android APIs.
 */
interface ParkingAlarmScheduler {
    suspend fun scheduleParkingAlarm(
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int,
        spotNote: String,
        use12h: Boolean
    ): Long

    fun scheduleExpiryAlarm(expiryMs: Long, spotNote: String, use12h: Boolean)
    fun cancelParkingAlarm()
    fun cancelExpiryAlarm()

    fun showInstantConfirmation(
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int,
        scheduledAlarmTimeStr: String,
        spotNote: String,
        use12h: Boolean
    )

    suspend fun extendTimer(additionalMinutes: Int): Boolean
    suspend fun cancelTimer()
}

/** Production implementation backed by [NotificationHelper] and [ParkingTimerManager]. */
class NotificationParkingScheduler(
    private val context: Context,
    private val settings: ParkingSettingsSource
) : ParkingAlarmScheduler {

    override suspend fun scheduleParkingAlarm(
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int,
        spotNote: String,
        use12h: Boolean
    ): Long = NotificationHelper.scheduleParkingAlarm(
        context = context,
        startTime = startTime,
        freeDurationMinutes = freeDurationMinutes,
        offsetMinutes = offsetMinutes,
        parkingSpotNote = spotNote,
        use12h = use12h
    )

    override fun scheduleExpiryAlarm(expiryMs: Long, spotNote: String, use12h: Boolean) {
        NotificationHelper.scheduleExpiryAlarm(
            context = context,
            expiryMs = expiryMs,
            parkingSpotNote = spotNote,
            use12h = use12h
        )
    }

    override fun cancelParkingAlarm() = NotificationHelper.cancelParkingAlarm(context)

    override fun cancelExpiryAlarm() = NotificationHelper.cancelExpiryAlarm(context)

    override fun showInstantConfirmation(
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int,
        scheduledAlarmTimeStr: String,
        spotNote: String,
        use12h: Boolean
    ) {
        NotificationHelper.showInstantConfirmationNotification(
            context = context,
            startTime = startTime,
            freeDurationMinutes = freeDurationMinutes,
            offsetMinutes = offsetMinutes,
            scheduledAlarmTimeStr = scheduledAlarmTimeStr,
            parkingSpotNote = spotNote,
            use12h = use12h
        )
    }

    override suspend fun extendTimer(additionalMinutes: Int): Boolean =
        ParkingTimerManager.extendTimer(context, settings, additionalMinutes)

    override suspend fun cancelTimer() {
        ParkingTimerManager.cancelTimer(context, settings)
    }
}
