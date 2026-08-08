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
     * Validates a parking schedule against [now]. Returns a user-facing error message when the
     * schedule cannot be honoured — malformed start time, non-positive free duration, or the free
     * window has already elapsed (expiry is in the past) — or null when it is schedulable.
     *
     * There is deliberately no next-day roll here: a window whose expiry already passed is an
     * error, never a silently scheduled ~24h timer.
     */
    fun validateSchedule(
        startTime: String,
        freeMinutes: Int,
        now: Long = System.currentTimeMillis()
    ): String? {
        val parts = startTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return "Enter a valid start time (HH:mm)"
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return "Enter a valid start time (HH:mm)"
        if (hour !in 0..23 || minute !in 0..59) return "Enter a valid start time (HH:mm)"
        if (freeMinutes <= 0) return "Free duration must be greater than 0 minutes"
        val expiry = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, freeMinutes)
        }.timeInMillis
        return if (expiry <= now) {
            "Free parking period has already expired - adjust the start time or free duration"
        } else {
            null
        }
    }

    /**
     * Wall-clock expiry millis for a parking session starting at "HH:mm" and lasting [freeMinutes],
     * interpreted on the current day. There is deliberately no next-day roll: if the computed time
     * is already in the past the caller must treat it as an error (see [validateSchedule]).
     */
    fun computeExpiryMillis(startTime: String, freeMinutes: Int): Long {
        val parts = startTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, freeMinutes)
        }.timeInMillis
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

    /**
     * Fraction of the free-parking window still remaining, for a circular countdown:
     * 1.0 at the start, depleting to 0.0 at expiry. Clamped to 0..1. An inactive or
     * zero-length timer reports a full ring (1.0).
     */
    fun remainingFraction(expiryMs: Long, totalMs: Long, nowMs: Long): Float {
        if (expiryMs <= 0L || totalMs <= 0L) return 1f
        val remainingMs = (expiryMs - nowMs).coerceIn(0L, totalMs)
        return remainingMs.toFloat() / totalMs.toFloat()
    }

    /**
     * Length of the countdown window in millis. Prefers the persisted free duration; falls back
     * to `expiry - start-of-day(startTime)` for legacy timers that were persisted without a
     * duration, so the progress ring can never be stuck at a full circle while a timer runs.
     * Returns 0 if neither source is available.
     */
    fun countdownTotalMs(freeDurationMinutes: Int, startTime: String, expiryMs: Long): Long {
        if (freeDurationMinutes > 0) return freeDurationMinutes * 60_000L
        val parts = startTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return 0L
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return 0L
        val startToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return (expiryMs - startToday).coerceAtLeast(0L)
    }
}
