package com.mndublo.odolens.data

import kotlinx.coroutines.flow.Flow

/**
 * Parking-feature view of the persisted settings, so the feature layer can be
 * unit-tested against a fake instead of the Android DataStore implementation.
 */
interface ParkingSettingsSource {
    val geminiApiKey: Flow<String>
    val notificationOffsetMinutes: Flow<Int>
    val use12HourFormat: Flow<Boolean>
    val parkingExpiryMs: Flow<Long>
    val parkingAlarmTime: Flow<String>
    val parkingSpotNote: Flow<String>
    val parkingStartTime: Flow<String>
    val parkingFreeDuration: Flow<Int>
    val parkingOffsetMinutes: Flow<Int>
    val parkingPlaceDirectory: Flow<List<ParkingPlace>>
    val parkingIsExpired: Flow<Boolean>

    suspend fun saveNotificationOffsetMinutes(minutes: Int)
    suspend fun saveParkingPlaceDirectory(places: List<ParkingPlace>)
    suspend fun setParkingExpired(expired: Boolean)

    suspend fun saveParkingTimer(
        expiryMs: Long,
        alarmTime: String,
        spotNote: String,
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int
    )

    suspend fun clearParkingTimer()
}
