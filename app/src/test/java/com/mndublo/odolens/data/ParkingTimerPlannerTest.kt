package com.mndublo.odolens.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkingTimerPlannerTest {

    @Test
    fun `newFreeDuration caps at 12 hours`() {
        assertEquals(60, ParkingTimerPlanner.newFreeDuration(0, 60))
        assertEquals(720, ParkingTimerPlanner.newFreeDuration(700, 60))
        assertEquals(720, ParkingTimerPlanner.newFreeDuration(720, 60))
        assertEquals(720, ParkingTimerPlanner.newFreeDuration(600, 500))
    }

    @Test
    fun `computeExpiryMillis rolls to next day when already past`() {
        // "09:00" + 60 minutes == 10:00; with `now` = noon today the expiry is still ahead
        val now = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 12)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val expiry = ParkingTimerPlanner.computeExpiryMillis("09:00", 60, now = now)
        val hourOfDay = java.util.Calendar.getInstance().apply { timeInMillis = expiry }
            .get(java.util.Calendar.HOUR_OF_DAY)
        // 09:00 + 1h is before noon, so same-day 10:00
        assertEquals(10, hourOfDay)
    }

    @Test
    fun `computeExpiryMillis bumps to next day when result is in the past`() {
        // "13:00" + 60 minutes == 14:00; with `now` = 15:00 today the expiry already passed -> next day 14:00
        val now = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 15)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val expiry = ParkingTimerPlanner.computeExpiryMillis("13:00", 60, now = now)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = expiry }
        // Day must be tomorrow relative to `now`'s day
        val nowDay = java.util.Calendar.getInstance().apply { timeInMillis = now }
            .get(java.util.Calendar.DAY_OF_YEAR)
        val expiryDay = cal.get(java.util.Calendar.DAY_OF_YEAR)
        assertEquals(nowDay + 1, expiryDay)
        assertEquals(14, cal.get(java.util.Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `formatCountdown shows expiry text when elapsed`() {
        assertEquals("Expired!", ParkingTimerPlanner.formatCountdown(0))
        assertEquals("Expired!", ParkingTimerPlanner.formatCountdown(-5000))
    }

    @Test
    fun `formatCountdown formats hours minutes seconds`() {
        assertEquals("02:00:00", ParkingTimerPlanner.formatCountdown(2 * 3600 * 1000))
        assertEquals("00:05", ParkingTimerPlanner.formatCountdown(5 * 1000))
        assertEquals("01:59:59", ParkingTimerPlanner.formatCountdown(2 * 3600 * 1000 - 1000))
    }

    @Test
    fun `formatExpiryDisplay is stable for same-day preview`() {
        // Same-day "09:00" + 60 -> "10:00" in 24h format
        assertEquals("10:00", ParkingTimerPlanner.formatExpiryDisplay("09:00", 60, use12h = false))
    }

    @Test
    fun `formatExpiryDisplay tolerates garbage input`() {
        // Matches the original behavior: unparseable input degrades to 00:00 rather than crashing
        assertEquals("00:00", ParkingTimerPlanner.formatExpiryDisplay("nope", 0, use12h = false))
    }

    @Test
    fun `formatExpiryDisplay shows AM PM in 12h mode`() {
        assertEquals("10:00 AM", ParkingTimerPlanner.formatExpiryDisplay("09:00", 60, use12h = true))
        assertEquals("2:30 PM", ParkingTimerPlanner.formatExpiryDisplay("13:30", 60, use12h = true))
        assertEquals("12:05 AM", ParkingTimerPlanner.formatExpiryDisplay("23:05", 60, use12h = true))
    }

    @Test
    fun `remainingFraction depletes from 1 to 0 across the window`() {
        val expiry = 3_600_000L
        val total = 3_600_000L
        assertEquals(1f, ParkingTimerPlanner.remainingFraction(expiry, total, 0L))
        assertEquals(0.5f, ParkingTimerPlanner.remainingFraction(expiry, total, 1_800_000L), 0.001f)
        assertEquals(0f, ParkingTimerPlanner.remainingFraction(expiry, total, expiry))
    }

    @Test
    fun `remainingFraction clamps past-expiry and inactive timers`() {
        val expiry = 3_600_000L
        val total = 3_600_000L
        // After expiry it never goes negative
        assertEquals(0f, ParkingTimerPlanner.remainingFraction(expiry, total, expiry + 10_000L))
        // No timer or no duration -> full ring
        assertEquals(1f, ParkingTimerPlanner.remainingFraction(0L, total, 0L))
        assertEquals(1f, ParkingTimerPlanner.remainingFraction(expiry, 0L, 0L))
    }

    @Test
    fun `countdownTotalMs prefers the free duration`() {
        val expiry = 3_600_000L
        assertEquals(
            3_600_000L,
            ParkingTimerPlanner.countdownTotalMs(freeDurationMinutes = 60, startTime = "09:00", expiryMs = expiry)
        )
    }

    @Test
    fun `countdownTotalMs falls back to start-of-day window for legacy timers`() {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val expiry = cal.timeInMillis + 60 * 60_000L
        assertEquals(
            60 * 60_000L,
            ParkingTimerPlanner.countdownTotalMs(freeDurationMinutes = 0, startTime = "09:00", expiryMs = expiry)
        )
        // Garbage start time or no expiry -> unknown window (0)
        assertEquals(0L, ParkingTimerPlanner.countdownTotalMs(0, "nope", expiry))
        assertEquals(0L, ParkingTimerPlanner.countdownTotalMs(0, "09:00", 0L))
    }
}
