package com.mndublo.odolens.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {

    /**
     * Format a HH:mm 24-hour string into either 24h or 12h display.
     * Input [startTime] is always in "HH:mm" format (from Gemini / user input).
     */
    fun formatStartTime(startTime: String, use12h: Boolean): String {
        return try {
            val parts = startTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            formatTime(hour, minute, use12h)
        } catch (e: Exception) {
            startTime
        }
    }

    /**
     * Format raw hour + minute integers into either 24h or 12h display string.
     */
    fun formatTime(hour: Int, minute: Int, use12h: Boolean): String {
        return if (use12h) {
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        }
    }

    /**
     * Format a Unix timestamp (millis) as "dd MMM yyyy, HH:mm" or "dd MMM yyyy, h:mm a".
     */
    fun formatTimestamp(timestampMs: Long, use12h: Boolean): String {
        val pattern = if (use12h) "dd MMM yyyy, h:mm a" else "dd MMM yyyy, HH:mm"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    /**
     * Format a Unix timestamp (millis) as time-only: "HH:mm" or "h:mm a".
     */
    fun formatAlarmTime(timestampMs: Long, use12h: Boolean): String {
        val pattern = if (use12h) "h:mm a" else "HH:mm"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}
