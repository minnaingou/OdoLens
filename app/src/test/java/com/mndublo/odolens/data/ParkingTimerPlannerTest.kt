package com.mndublo.odolens.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun `computeExpiryMillis returns raw wall-clock expiry on the current day`() {
        // "09:00" + 60 minutes == 10:00 on the current day (no next-day roll)
        val expiry = ParkingTimerPlanner.computeExpiryMillis("09:00", 60)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = expiry }
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        assertEquals(today, cal.get(java.util.Calendar.DAY_OF_YEAR))
        assertEquals(10, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun `computeExpiryMillis does not roll a past expiry to the next day`() {
        // "13:00" + 60 minutes == 14:00; with `now` = 15:00 the expiry has already passed,
        // but the raw wall-clock time is still returned (validation is a separate concern)
        val now = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 15)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val expiry = ParkingTimerPlanner.computeExpiryMillis("13:00", 60)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = expiry }
        val nowDay = java.util.Calendar.getInstance().apply { timeInMillis = now }
            .get(java.util.Calendar.DAY_OF_YEAR)
        // Same day as `now`, NOT tomorrow
        assertEquals(nowDay, cal.get(java.util.Calendar.DAY_OF_YEAR))
        assertEquals(14, cal.get(java.util.Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `validateSchedule accepts a window that is still open`() {
        // now = 15:00; start 16:00 + 60 min == 17:00 -> still in the future
        val now = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 15)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertNull(ParkingTimerPlanner.validateSchedule("16:00", 60, now = now))
    }

    @Test
    fun `validateSchedule rejects an already-elapsed free window`() {
        // now = 15:00; start 13:00 + 60 min == 14:00 -> expiry already passed
        val now = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 15)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val error = ParkingTimerPlanner.validateSchedule("13:00", 60, now = now)
        assertNotNull(error)
        assertTrue(error.orEmpty().contains("already expired"))
    }

    @Test
    fun `validateSchedule rejects a window expiring exactly now`() {
        // The reported bug scenario: start 30 min ago (14:30) with 30 min free -> expiry 15:00 == now
        val now = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 15)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertNotNull(ParkingTimerPlanner.validateSchedule("14:30", 30, now = now))
    }

    @Test
    fun `validateSchedule rejects malformed input and non-positive durations`() {
        val now = java.util.Calendar.getInstance().timeInMillis
        assertNotNull(ParkingTimerPlanner.validateSchedule("nope", 60, now = now))
        assertNotNull(ParkingTimerPlanner.validateSchedule("10:00", 0, now = now))
        assertNotNull(ParkingTimerPlanner.validateSchedule("10:00", -5, now = now))
    }

    @Test
    fun `validateSchedule rejects out-of-range times`() {
        val now = java.util.Calendar.getInstance().timeInMillis
        assertNotNull(ParkingTimerPlanner.validateSchedule("24:00", 60, now = now))
        assertNotNull(ParkingTimerPlanner.validateSchedule("12:60", 60, now = now))
        assertNotNull(ParkingTimerPlanner.validateSchedule("99:99", 60, now = now))
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
