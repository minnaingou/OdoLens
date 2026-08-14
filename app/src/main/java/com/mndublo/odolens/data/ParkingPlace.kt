package com.mndublo.odolens.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A single entry in the user's Parking Place Directory.
 *
 * @param id          Stable UUID string — survives edits so selection state can track by id.
 * @param name        Human-readable place name (e.g. "The Mall Bangkapi").
 * @param freeMinutes Free parking window in minutes (e.g. 240 = 4 h).
 */
@Serializable
data class ParkingPlace(
    val id: String,
    val name: String,
    val freeMinutes: Int,
    val lastUsedEpochMs: Long = 0L
)

/** Encode / decode [ParkingPlace] lists to/from a single JSON string for DataStore storage. */
object ParkingPlaceSerializer {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(places: List<ParkingPlace>): String =
        json.encodeToString(places)

    fun decode(raw: String): List<ParkingPlace> =
        runCatching { json.decodeFromString<List<ParkingPlace>>(raw) }.getOrDefault(emptyList())
}
