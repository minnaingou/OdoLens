package com.mndublo.odolens.data

import kotlinx.coroutines.flow.Flow

/** Persistence for the saved-trips list (unit-testable seam). */
interface TripStore {
    val trips: Flow<List<Trip>>

    suspend fun saveTrip(trip: Trip)

    suspend fun deleteTrip(tripId: String)
}
