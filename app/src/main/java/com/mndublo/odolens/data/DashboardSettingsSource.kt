package com.mndublo.odolens.data

import kotlinx.coroutines.flow.Flow

/** Dashboard-feature view of the persisted settings (unit-testable seam). */
interface DashboardSettingsSource {
    val fuelPrice: Flow<Double>
    val fuelPriceDate: Flow<String>
    val use12HourFormat: Flow<Boolean>
    val geminiApiKey: Flow<String>

    suspend fun saveFuelPrice(price: Double)
}
