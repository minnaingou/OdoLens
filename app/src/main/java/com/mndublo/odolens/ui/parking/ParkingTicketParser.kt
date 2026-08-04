package com.mndublo.odolens.ui.parking

import android.graphics.Bitmap
import com.mndublo.odolens.api.GeminiClient
import com.mndublo.odolens.api.ParkingTicketData

/** AI parsing of a parking-ticket photo. Abstracted so [ParkingViewModel] is testable. */
interface ParkingTicketParser {
    suspend fun parse(apiKey: String, bitmap: Bitmap?): Result<ParkingTicketData>
}

object GeminiParkingTicketParser : ParkingTicketParser {
    override suspend fun parse(apiKey: String, bitmap: Bitmap?): Result<ParkingTicketData> =
        if (bitmap == null) {
            Result.failure(IllegalArgumentException("No image to parse"))
        } else {
            GeminiClient.parseParkingTicket(apiKey, bitmap)
        }
}
