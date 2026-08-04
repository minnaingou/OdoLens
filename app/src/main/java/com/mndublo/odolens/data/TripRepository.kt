package com.mndublo.odolens.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class TripRepository(private val context: Context) : TripStore {
    private val file = File(context.filesDir, "trips.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    override val trips: Flow<List<Trip>> = _trips.asStateFlow()

    init {
        loadTrips()
    }

    private fun loadTrips() {
        try {
            if (file.exists()) {
                val content = file.readText()
                val list = json.decodeFromString<List<Trip>>(content)
                _trips.value = list.sortedByDescending { it.timestamp }
            } else {
                _trips.value = emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _trips.value = emptyList()
        }
    }

    override suspend fun saveTrip(trip: Trip) = withContext(Dispatchers.IO) {
        val currentList = _trips.value.toMutableList()
        currentList.add(trip)
        writeTripsToFile(currentList)
    }

    override suspend fun deleteTrip(tripId: String) = withContext(Dispatchers.IO) {
        val currentList = _trips.value.filter { it.id != tripId }
        writeTripsToFile(currentList)
    }

    private fun writeTripsToFile(list: List<Trip>) {
        try {
            val content = json.encodeToString(list)
            file.writeText(content)
            _trips.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
