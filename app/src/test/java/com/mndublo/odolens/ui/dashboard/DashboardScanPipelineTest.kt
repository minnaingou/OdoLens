package com.mndublo.odolens.ui.dashboard

import android.graphics.Bitmap
import com.mndublo.odolens.ocr.DashboardData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeOcr(
    private val parsed: DashboardData,
    private val valid: Boolean
) : DashboardOcr {
    override suspend fun recognizeText(bitmap: Bitmap?): String = "fake raw text"
    override fun parseDashboardText(text: String): DashboardData = parsed
    override fun isDashboardDataValid(data: DashboardData): Boolean = valid
}

private class FakeGemini(
    private val result: Result<DashboardData>
) : DashboardGeminiParser {
    override suspend fun parse(apiKey: String, bitmap: Bitmap?): Result<DashboardData> = result
}

class DashboardScanPipelineTest {

    private fun pipeline(ocr: DashboardOcr, gemini: DashboardGeminiParser) =
        DashboardScanPipeline(ocr, gemini)

    @Test
    fun `valid OCR result populates distance and economy without touching Gemini`() = runTest {
        val ocr = FakeOcr(DashboardData(120.5, 15.4), valid = true)
        val gemini = FakeGemini(Result.success(DashboardData(999.0, 999.0)))

        val outcome = pipeline(ocr, gemini).process(apiKey = "", bitmap = null)

        assertTrue(outcome is ScanOutcome.Success)
        outcome as ScanOutcome.Success
        assertEquals("120.5", outcome.distanceKm)
        assertEquals("15.4", outcome.fuelEconomyKmL)
    }

    @Test
    fun `invalid OCR with blank api key fails with settings hint`() = runTest {
        val ocr = FakeOcr(DashboardData(null, null), valid = false)
        val gemini = FakeGemini(Result.success(DashboardData(1.0, 1.0)))

        val outcome = pipeline(ocr, gemini).process(apiKey = "", bitmap = null)

        assertTrue(outcome is ScanOutcome.Failure)
        assertEquals(
            "OCR failed and no Gemini API key is set. Please add your key in Settings.",
            (outcome as ScanOutcome.Failure).message
        )
    }

    @Test
    fun `invalid OCR falls back to Gemini and uses its values`() = runTest {
        val ocr = FakeOcr(DashboardData(null, null), valid = false)
        val gemini = FakeGemini(Result.success(DashboardData(88.0, 12.3)))

        val outcome = pipeline(ocr, gemini).process(apiKey = "secret", bitmap = null)

        assertTrue(outcome is ScanOutcome.Success)
        outcome as ScanOutcome.Success
        assertEquals("88.0", outcome.distanceKm)
        assertEquals("12.3", outcome.fuelEconomyKmL)
    }

    @Test
    fun `Gemini returning null fields maps to manual-entry failure`() = runTest {
        val ocr = FakeOcr(DashboardData(null, null), valid = false)
        val gemini = FakeGemini(Result.success(DashboardData(null, null)))

        val outcome = pipeline(ocr, gemini).process(apiKey = "secret", bitmap = null)

        assertTrue(outcome is ScanOutcome.Failure)
        assertEquals(
            "Gemini could not detect distance or economy from this image. Please enter manually.",
            (outcome as ScanOutcome.Failure).message
        )
    }

    @Test
    fun `Gemini failure maps to AI parse failure`() = runTest {
        val ocr = FakeOcr(DashboardData(null, null), valid = false)
        val gemini = FakeGemini(Result.failure(Exception("boom")))

        val outcome = pipeline(ocr, gemini).process(apiKey = "secret", bitmap = null)

        assertTrue(outcome is ScanOutcome.Failure)
        assertEquals("AI Parse failed: boom", (outcome as ScanOutcome.Failure).message)
    }
}
