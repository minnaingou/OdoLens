package com.mndublo.odolens.api

import android.graphics.Bitmap
import android.util.Base64
import com.mndublo.odolens.data.AppLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun parseParkingTicket(
        apiKey: String,
        bitmap: Bitmap
    ): Result<ParkingTicketData> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is required to analyze parking ticket image."))
        }

        val modelsToTry = listOf(
            "gemini-3.1-flash-lite",
            "gemini-3.6-flash"
        )
        
        var lastException: Exception? = null

        for (modelName in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                
                // Build prompt for direct image extraction in Thai/English
                val prompt = """
                    Analyze this parking ticket image directly (in Thai, English, or both). 
                    Identify:
                    1. The parking start time / check-in time / time in /เวลาเข้า (in HH:mm 24-hour format).
                    2. The free parking duration in minutes (look for "free parking", "free hour", "จอดฟรี", "ส่วนลดจอดรถ", etc.). If no free duration is specified or found, return 0. (e.g., 1 hour -> 60, 2 hours -> 120, 30 mins -> 30).
                    
                    Respond ONLY with a valid JSON object in this exact format with no extra text or markdown codeblocks:
                    {
                      "start_time": "HH:mm",
                      "free_duration_minutes": 120
                    }
                """.trimIndent()

                // Prepare JSON payload
                val root = JSONObject()
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                // Text part
                val textPartObj = JSONObject()
                textPartObj.put("text", prompt)
                partsArray.put(textPartObj)

                // Image part (resized & compressed for free tier efficiency)
                val scaledBitmap = bitmap.scaleForApi(maxDimension = 1024)
                val imagePartObj = JSONObject()
                val inlineDataObj = JSONObject()
                inlineDataObj.put("mimeType", "image/jpeg")
                inlineDataObj.put("data", scaledBitmap.toBase64())
                imagePartObj.put("inlineData", inlineDataObj)
                partsArray.put(imagePartObj)

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                root.put("contents", contentsArray)

                // System instructions / generation config for JSON output
                val generationConfig = JSONObject()
                generationConfig.put("responseMimeType", "application/json")
                root.put("generationConfig", generationConfig)

                val requestBody = root.toString().toRequestBody(JSON_MEDIA_TYPE)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 404) {
                        // Model endpoint not found, try next model in fallback list
                        lastException = Exception("Model $modelName not found (404)")
                        return@use
                    }
                    if (response.code == 429) {
                        return@withContext Result.failure(Exception("Gemini Free Tier rate limit reached (HTTP 429). Please wait a few seconds and try again."))
                    }
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("API Error (${response.code}): ${response.message}"))
                    }
                    val bodyStr = response.body?.string() ?: ""
                    val responseJson = JSONObject(bodyStr)
                    
                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        return@withContext Result.failure(Exception("No parsing results from Gemini"))
                    }
                    
                    val textResponse = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    
                    AppLogger.log("Gemini raw text:\n$textResponse")

                    // Map the JSON into a ticket, failing when the model couldn't actually
                    // read the image (missing/blank/placeholder fields) so the user gets an
                    // error card instead of a silent garbage fill. A bad parse tries the
                    // next model in the fallback list.
                    val parseOutcome = parseParkingTicketJson(textResponse)
                    if (parseOutcome.isFailure) {
                        lastException = parseOutcome.exceptionOrNull() as? Exception
                        return@use
                    }
                    return@withContext parseOutcome
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        
        return@withContext Result.failure(lastException ?: Exception("Failed to connect to Gemini API."))
    }

    suspend fun parseDashboardImage(
        apiKey: String,
        bitmap: Bitmap
    ): Result<com.mndublo.odolens.ocr.DashboardData> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is required. Please add it in Settings."))
        }

        val modelsToTry = listOf(
            "gemini-2.5-flash",
            "gemini-3.1-flash-lite",
            "gemini-3.6-flash"
        )

        var lastException: Exception? = null

        for (modelName in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

                val prompt = """
                    Analyze this vehicle dashboard / trip computer image.
                    Extract:
                    1. Trip distance in kilometers (look for odometer trip reading, distance display, "km" label). Preserve exact decimal points if present (e.g. 120.5 or 120).
                    2. Fuel economy / fuel consumption in km/L (look for "km/L", "L/100km" converted to km/L, or similar). Preserve exact decimal points if present (e.g. 15.4 or 15).

                    Respond ONLY with a valid JSON object in this exact format with no extra text or markdown:
                    {
                      "distance_km": 120.5,
                      "fuel_economy_kmL": 15.4
                    }

                    If a value cannot be found, use 0.
                """.trimIndent()

                val root = JSONObject()
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                val textPartObj = JSONObject()
                textPartObj.put("text", prompt)
                partsArray.put(textPartObj)

                val scaledBitmap = bitmap.scaleForApi(maxDimension = 1024)
                val imagePartObj = JSONObject()
                val inlineDataObj = JSONObject()
                inlineDataObj.put("mimeType", "image/jpeg")
                inlineDataObj.put("data", scaledBitmap.toBase64())
                imagePartObj.put("inlineData", inlineDataObj)
                partsArray.put(imagePartObj)

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                root.put("contents", contentsArray)

                val generationConfig = JSONObject()
                generationConfig.put("responseMimeType", "application/json")
                root.put("generationConfig", generationConfig)

                val requestBody = root.toString().toRequestBody(JSON_MEDIA_TYPE)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 404) {
                        lastException = Exception("Model $modelName not found (404)")
                        return@use
                    }
                    if (response.code == 429) {
                        return@withContext Result.failure(Exception("Gemini Free Tier rate limit reached (HTTP 429). Please wait a few seconds and try again."))
                    }
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("API Error (${response.code}): ${response.message}"))
                    }
                    val bodyStr = response.body?.string() ?: ""
                    val responseJson = JSONObject(bodyStr)

                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        return@withContext Result.failure(Exception("No results from Gemini"))
                    }

                    val textResponse = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    AppLogger.log("Gemini Dashboard raw response:\n$textResponse")

                    val parsedJson = JSONObject(textResponse.trim())
                    val distanceKm = parsedJson.optDouble("distance_km", 0.0).let { if (it == 0.0) null else it }
                    val fuelEconomy = parsedJson.optDouble("fuel_economy_kmL", 0.0).let { if (it == 0.0) null else it }

                    return@withContext Result.success(
                        com.mndublo.odolens.ocr.DashboardData(distanceKm, fuelEconomy)
                    )
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        return@withContext Result.failure(lastException ?: Exception("Failed to connect to Gemini API."))
    }

    private fun Bitmap.scaleForApi(maxDimension: Int): Bitmap {
        val width = this.width
        val height = this.height
        if (width <= maxDimension && height <= maxDimension) return this
        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    private fun parseParkingTicketFallback(text: String): ParkingTicketData {
        // Try to match HH:mm or HH.mm pattern for start time
        val timeRegex = Regex("""\b([01]?[0-9]|2[0-3])[:.]([0-5][0-9])\b""")
        val timeMatch = timeRegex.find(text)
        val startTime = timeMatch?.value?.replace('.', ':') ?: "00:00"

        // Search for numbers followed by hours/minutes to estimate free duration (English/Thai keywords)
        var freeMinutes = 0
        val hourRegex = Regex("""(\d+)\s*(?:hour|hr|ชั่วโมง|ชม)""", RegexOption.IGNORE_CASE)
        val minuteRegex = Regex("""(\d+)\s*(?:minute|min|นาที)""", RegexOption.IGNORE_CASE)

        val hourMatch = hourRegex.find(text)
        if (hourMatch != null) {
            freeMinutes = (hourMatch.groupValues[1].toIntOrNull() ?: 0) * 60
        } else {
            val minMatch = minuteRegex.find(text)
            if (minMatch != null) {
                freeMinutes = minMatch.groupValues[1].toIntOrNull() ?: 0
            }
        }

        return ParkingTicketData(startTime, freeMinutes)
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}

data class ParkingTicketData(
    val startTime: String, // HH:mm format
    val freeDurationMinutes: Int
)

/** Human-facing failure message when the model can't read a parking-ticket image. */
private const val PARKING_TICKET_UNREADABLE_MESSAGE =
    "Could not read the parking ticket. Please try a clearer photo or enter the details manually."

/** Lenient HH:mm check (Gemini is instructed to return 24-hour HH:mm). */
private val HH_MM_PATTERN = Regex("""^\d{1,2}:\d{2}$""")

/**
 * Pure JSON → [ParkingTicketData] mapping for parking-ticket AI responses, kept
 * unit-testable. Returns failure when the model couldn't actually read the image:
 * missing fields, a blank or non-HH:mm start time, a negative duration, or the
 * "00:00 / 0" placeholder the model emits when it can't find anything on the ticket.
 */
internal fun parseParkingTicketJson(textResponse: String): Result<ParkingTicketData> = try {
    val parsedJson = JSONObject(textResponse.trim())

    if (!parsedJson.has("start_time") || !parsedJson.has("free_duration_minutes")) {
        return Result.failure(Exception(PARKING_TICKET_UNREADABLE_MESSAGE))
    }

    val startTime = parsedJson.getString("start_time").trim()
    val freeDuration = parsedJson.optInt("free_duration_minutes", -1)

    val unreadable = startTime.isBlank() ||
        !HH_MM_PATTERN.matches(startTime) ||
        freeDuration < 0 ||
        // The model's default response when it can't read the image at all.
        (startTime == "00:00" && freeDuration == 0)

    if (unreadable) {
        return Result.failure(Exception(PARKING_TICKET_UNREADABLE_MESSAGE))
    }

    Result.success(ParkingTicketData(startTime, freeDuration))
} catch (e: Exception) {
    Result.failure(Exception(PARKING_TICKET_UNREADABLE_MESSAGE, e))
}
