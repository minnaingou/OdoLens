package com.mndublo.odolens.data

import java.util.Calendar
import java.util.Locale

/**
 * Pure parking-timer domain rules: duration cap, expiry math and formatting.
 * No Android dependencies — unit-testable in plain JVM.
 */
object ParkingTimerPlanner {

    /** Maximum total free-parking duration (12 hours) in minutes. */
    const val MAX_FREE_MINUTES = 12 * 60

    /** Caps [additionalMinutes] added to [currentFreeMinutes] at the 12-hour limit. */
    fun newFreeDuration(currentFreeMinutes: Int, additionalMinutes: Int): Int =
        minOf(currentFreeMinutes + additionalMinutes, MAX_FREE_MINUTES)

    /**
     * Wall-clock expiry millis for a parking session starting at "HH:mm" and lasting [freeMinutes].
     * If the computed time is already in the past it is rolled to the next day.
     */
    fun computeExpiryMillis(
        startTime: String,
        freeMinutes: Int,
        now: Long = System.currentTimeMillis()
    ): Long {
        val parts = startTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, freeMinutes)
            if (timeInMillis <= now) add(Calendar.DATE, 1)
        }
        return cal.timeInMillis
    }

    /** "HH:mm" expiry display for the form preview (no next-day roll). */
    fun formatExpiryDisplay(startTime: String, freeMinutes: Int, use12h: Boolean): String {
        return try {
            val parts = startTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, freeMinutes)
            }
            TimeFormatter.formatTime(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                use12h
            )
        } catch (e: Exception) {
            "--:--"
        }
    }

    /** Live countdown from a remaining-millis delta: "HH:mm:ss" / "mm:ss", or "Expired!". */
    fun formatCountdown(remainingMs: Long): String {
        if (remainingMs <= 0) return "Expired!"
        val totalSec = remainingMs / 1000
        val hours = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }
    }
}
