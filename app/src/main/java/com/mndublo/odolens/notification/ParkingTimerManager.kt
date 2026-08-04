package com.mndublo.odolens.notification

import android.content.Context
import com.mndublo.odolens.data.ParkingSettingsSource
import com.mndublo.odolens.data.ParkingTimerPlanner
import com.mndublo.odolens.data.TimeFormatter
import kotlinx.coroutines.flow.first

/**
 * Shared parking timer operations used by both the UI (ParkingScreen)
 * and the notification BroadcastReceiver (ParkingExtendReceiver).
 *
 * All functions are suspend and must be called from a coroutine.
 */
object ParkingTimerManager {

    /**
     * Extends the active parking timer by [additionalMinutes].
     * Enforces a 12-hour cap on total free duration.
     * Cancels the old OS alarm, schedules a new one, and updates the
     * ongoing notification and DataStore state.
     *
     * @return true if extension was applied, false if no timer is active or cap reached.
     */
    suspend fun extendTimer(
        context: Context,
        settings: ParkingSettingsSource,
        additionalMinutes: Int
    ): Boolean {
        // Read current persisted state
        val startTime = settings.parkingStartTime.first()
        val currentFree = settings.parkingFreeDuration.first()
        val offset = settings.parkingOffsetMinutes.first()
        val spotNote = settings.parkingSpotNote.first()
        val expiryMs = settings.parkingExpiryMs.first()
        val use12h = settings.use12HourFormat.first()

        if (expiryMs == 0L || startTime.isBlank()) return false

        // 12-hour cap on total free duration
        val newFree = ParkingTimerPlanner.newFreeDuration(currentFree, additionalMinutes)
        if (newFree == currentFree) return false // already at cap

        // Cancel the existing OS alarm
        NotificationHelper.cancelParkingAlarm(context)

        // Schedule a new alarm with the updated free duration
        val newAlarmMs = NotificationHelper.scheduleParkingAlarm(
            context = context,
            startTime = startTime,
            freeDurationMinutes = newFree,
            offsetMinutes = offset,
            parkingSpotNote = spotNote,
            use12h = use12h
        )

        val newAlarmStr = TimeFormatter.formatAlarmTime(newAlarmMs, use12h)

        // Recalculate expiry wall-clock time (startTime + newFree)
        val newExpiryMs = ParkingTimerPlanner.computeExpiryMillis(startTime, newFree)

        // Schedule expiry alarm at exact expiry moment
        NotificationHelper.scheduleExpiryAlarm(
            context = context,
            expiryMs = newExpiryMs,
            parkingSpotNote = spotNote,
            use12h = use12h
        )

        // Persist all updated state (including new freeDuration)
        settings.saveParkingTimer(
            expiryMs = newExpiryMs,
            alarmTime = newAlarmStr,
            spotNote = spotNote,
            startTime = startTime,
            freeDurationMinutes = newFree,
            offsetMinutes = offset
        )

        // Refresh the ongoing status notification with new times + action buttons
        NotificationHelper.showInstantConfirmationNotification(
            context = context,
            startTime = startTime,
            freeDurationMinutes = newFree,
            offsetMinutes = offset,
            scheduledAlarmTimeStr = newAlarmStr,
            parkingSpotNote = spotNote,
            use12h = use12h
        )

        return true
    }

    /**
     * Cancels the active parking timer: cancels the OS alarm, clears
     * notifications, and wipes DataStore state.
     */
    suspend fun cancelTimer(
        context: Context,
        settings: ParkingSettingsSource
    ) {
        NotificationHelper.cancelParkingAlarm(context)
        NotificationHelper.cancelExpiryAlarm(context)
        settings.clearParkingTimer()
    }
}
