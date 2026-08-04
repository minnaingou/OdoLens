package com.mndublo.odolens.ui.dashboard

import android.graphics.Bitmap
import com.mndublo.odolens.api.GeminiClient
import com.mndublo.odolens.data.AppLogger
import com.mndublo.odolens.ocr.DashboardData
import com.mndublo.odolens.ocr.OCRManager

/** Local ML-Kit OCR primitives, abstracted so the fallback decision logic is unit-testable. */
interface DashboardOcr {
    suspend fun recognizeText(bitmap: Bitmap?): String
    fun parseDashboardText(text: String): DashboardData
    fun isDashboardDataValid(data: DashboardData): Boolean
}

object OcrManagerAdapter : DashboardOcr {
    override suspend fun recognizeText(bitmap: Bitmap?): String =
        bitmap?.let { OCRManager.recognizeText(it).getOrNull() } ?: ""

    override fun parseDashboardText(text: String): DashboardData = OCRManager.parseDashboardText(text)

    override fun isDashboardDataValid(data: DashboardData): Boolean = OCRManager.isDashboardDataValid(data)
}

/** Gemini fallback parser, abstracted so the pipeline stays testable. */
interface DashboardGeminiParser {
    suspend fun parse(apiKey: String, bitmap: Bitmap?): Result<DashboardData>
}

object GeminiDashboardParser : DashboardGeminiParser {
    override suspend fun parse(apiKey: String, bitmap: Bitmap?): Result<DashboardData> =
        if (bitmap == null) {
            Result.failure(IllegalArgumentException("No image to parse"))
        } else {
            GeminiClient.parseDashboardImage(apiKey, bitmap)
        }
}

sealed interface ScanOutcome {
    data class Success(val distanceKm: String, val fuelEconomyKmL: String) : ScanOutcome
    data class Failure(val message: String) : ScanOutcome
}

/**
 * OCR-first → Gemini-fallback decision pipeline for dashboard photos.
 * Moved out of the composable so the fallback rules are unit-testable.
 */
class DashboardScanPipeline(
    private val ocr: DashboardOcr,
    private val gemini: DashboardGeminiParser
) {
    suspend fun process(apiKey: String, bitmap: Bitmap?): ScanOutcome {
        // Note: the ViewModel guarantees a non-null bitmap before calling this; the parameter is
        // nullable only so the fallback logic can be unit-tested on the JVM (fakes ignore it).
        val rawText = ocr.recognizeText(bitmap)
        AppLogger.log("Dashboard OCR raw text:\n$rawText")

        val parsed = ocr.parseDashboardText(rawText)
        AppLogger.log("OCR parsed: distance=${parsed.distanceKm}, economy=${parsed.fuelEconomyKmL}")

        if (ocr.isDashboardDataValid(parsed)) {
            return ScanOutcome.Success(parsed.distanceKm.toString(), parsed.fuelEconomyKmL.toString())
        }

        // OCR did not find the required data — fall back to Gemini
        AppLogger.log("OCR result insufficient, falling back to Gemini AI")
        if (apiKey.isBlank()) {
            return ScanOutcome.Failure(
                "OCR failed and no Gemini API key is set. Please add your key in Settings."
            )
        }

        return gemini.parse(apiKey, bitmap).fold(
            onSuccess = { data ->
                if (data.distanceKm == null && data.fuelEconomyKmL == null) {
                    ScanOutcome.Failure(
                        "Gemini could not detect distance or economy from this image. Please enter manually."
                    )
                } else {
                    ScanOutcome.Success(
                        data.distanceKm?.toString() ?: "",
                        data.fuelEconomyKmL?.toString() ?: ""
                    )
                }
            },
            onFailure = { err -> ScanOutcome.Failure("AI Parse failed: ${err.message}") }
        )
    }
}
