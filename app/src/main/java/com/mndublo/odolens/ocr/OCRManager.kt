package com.mndublo.odolens.ocr

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OCRManager {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(bitmap: Bitmap): Result<String> = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val task = recognizer.process(image)
            val result = Tasks.await(task)
            Result.success(result.text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Strict parse of dashboard OCR text.
     *
     * Rules (per user requirement):
     *  - Distance: number containing a dot (.) immediately followed by "km" (not "km/L").
     *  - Economy: number containing a dot (.) immediately followed by "km/L", "km\L", or "kmL".
     *
     * Returns null fields if the number lacks a dot or pattern is not matched, signalling the caller to fall back to Gemini.
     */
    fun parseDashboardText(text: String): DashboardData {
        // Distance: number with a dot (\d+\.\d+) + km, NOT followed by /L or \L
        val distanceRegex = Regex("""(?i)(\d+\.\d+)\s*km(?![/\\]?l)""")
        val distanceMatch = distanceRegex.find(text)
        val distance = distanceMatch?.groupValues?.get(1)?.toDoubleOrNull()

        // Economy: number with a dot (\d+\.\d+) + km/L, km\L, or kmL
        val economyRegex = Regex("""(?i)(\d+\.\d+)\s*km\s*[/\\]?\s*l""")
        val economyMatch = economyRegex.find(text)
        val fuelEconomy = economyMatch?.groupValues?.get(1)?.toDoubleOrNull()

        return DashboardData(distance, fuelEconomy)
    }

    /**
     * Returns true only if both distance and economy were successfully parsed.
     */
    fun isDashboardDataValid(data: DashboardData): Boolean =
        data.distanceKm != null && data.fuelEconomyKmL != null
}

data class DashboardData(
    val distanceKm: Double?,
    val fuelEconomyKmL: Double?
)
