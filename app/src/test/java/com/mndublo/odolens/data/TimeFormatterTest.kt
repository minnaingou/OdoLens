package com.mndublo.odolens.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `formatStartTime shows AM PM in 12h mode`() {
        assertEquals("10:00 AM", TimeFormatter.formatStartTime("10:00", use12h = true))
        assertEquals("2:30 PM", TimeFormatter.formatStartTime("14:30", use12h = true))
        assertEquals("12:00 PM", TimeFormatter.formatStartTime("12:00", use12h = true))
        assertEquals("12:05 AM", TimeFormatter.formatStartTime("00:05", use12h = true))
        assertEquals("11:59 PM", TimeFormatter.formatStartTime("23:59", use12h = true))
    }

    @Test
    fun `formatStartTime stays 24h in 24h mode`() {
        assertEquals("14:30", TimeFormatter.formatStartTime("14:30", use12h = false))
        assertEquals("00:05", TimeFormatter.formatStartTime("00:05", use12h = false))
    }

    @Test
    fun `formatTime handles noon and midnight boundaries in 12h mode`() {
        assertEquals("12:00 PM", TimeFormatter.formatTime(12, 0, use12h = true))
        assertEquals("12:00 AM", TimeFormatter.formatTime(0, 0, use12h = true))
    }
}
