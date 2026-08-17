package com.mndublo.odolens.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the parking-ticket AI response mapping. */
class GeminiClientParseTest {

    @Test
    fun `valid ticket JSON maps to ParkingTicketData`() {
        val result = parseParkingTicketJson("""{"start_time": "10:15", "free_duration_minutes": 120}""")

        assertTrue(result.isSuccess)
        assertEquals("10:15", result.getOrThrow().startTime)
        assertEquals(120, result.getOrThrow().freeDurationMinutes)
    }

    @Test
    fun `zero free duration is a valid result`() {
        val result = parseParkingTicketJson("""{"start_time": "14:30", "free_duration_minutes": 0}""")

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow().freeDurationMinutes)
    }

    @Test
    fun `single-digit hour is accepted`() {
        val result = parseParkingTicketJson("""{"start_time": "9:05", "free_duration_minutes": 60}""")

        assertTrue(result.isSuccess)
        assertEquals("9:05", result.getOrThrow().startTime)
    }

    @Test
    fun `missing duration field fails as unreadable`() {
        val result = parseParkingTicketJson("""{"start_time": "10:15"}""")

        assertTrue(result.isFailure)
    }

    @Test
    fun `blank start time fails as unreadable`() {
        val result = parseParkingTicketJson("""{"start_time": "", "free_duration_minutes": 60}""")

        assertTrue(result.isFailure)
    }

    @Test
    fun `non HH-mm start time fails as unreadable`() {
        val result = parseParkingTicketJson("""{"start_time": "ten fifteen", "free_duration_minutes": 60}""")

        assertTrue(result.isFailure)
    }

    @Test
    fun `placeholder default response fails as unreadable`() {
        // Gemini emits 00:00 / 0 when it cannot read anything from the image.
        val result = parseParkingTicketJson("""{"start_time": "00:00", "free_duration_minutes": 0}""")

        assertTrue(result.isFailure)
    }

    @Test
    fun `non JSON response fails`() {
        val result = parseParkingTicketJson("sorry, I cannot read this ticket image")

        assertTrue(result.isFailure)
    }
}
