package com.mndublo.odolens.data

import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    val id: String,
    val timestamp: Long,
    val name: String?,
    val distanceKm: Double,
    val fuelEconomyKmL: Double,
    val fuelPrice: Double,
    val cost: Double
)
