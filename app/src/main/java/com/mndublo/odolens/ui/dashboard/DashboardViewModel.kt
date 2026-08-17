package com.mndublo.odolens.ui.dashboard

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mndublo.odolens.data.DashboardSettingsSource
import com.mndublo.odolens.data.SettingsRepository
import com.mndublo.odolens.data.Trip
import com.mndublo.odolens.data.TripRepository
import com.mndublo.odolens.data.TripStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val trips: List<Trip> = emptyList(),
    val fuelPrice: Double = 35.0,
    val fuelPriceDate: String = "",
    val use12h: Boolean = false,
    val apiKey: String = "",
    // True once persisted settings have been read; gates the missing-key warning so it
    // doesn't flash on launch before the real value arrives.
    val settingsLoaded: Boolean = false,
    val distanceInput: String = "",
    val economyInput: String = "",
    val tripNameInput: String = "",
    val fuelPriceInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // One-shot UI feedback flag, consumed by the screen (haptic on scan completion)
    val scanFeedback: Boolean = false
) {
    /** Dynamic cost estimate for the form preview. */
    val calculatedCost: Double
        get() {
            val distance = distanceInput.toDoubleOrNull() ?: 0.0
            val economy = economyInput.toDoubleOrNull() ?: 0.0
            val price = fuelPriceInput.toDoubleOrNull() ?: fuelPrice
            return if (economy > 0.0) (distance / economy) * price else 0.0
        }
}

class DashboardViewModel(
    private val settings: DashboardSettingsSource,
    private val tripStore: TripStore,
    private val scanPipeline: DashboardScanPipeline
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /** In-flight scan job, so the user can cancel a scan from the progress card. */
    private var scanJob: Job? = null

    private var fuelPriceSeeded = false

    init {
        viewModelScope.launch {
            tripStore.trips.collect { trips -> _uiState.update { it.copy(trips = trips) } }
        }
        viewModelScope.launch {
            combine(settings.fuelPrice, settings.fuelPriceDate) { price, date -> price to date }
                .collect { (price, date) ->
                    _uiState.update { st ->
                        st.copy(
                            fuelPrice = price,
                            fuelPriceDate = date,
                            fuelPriceInput = if (!fuelPriceSeeded) price.toString() else st.fuelPriceInput
                        )
                    }
                    fuelPriceSeeded = true
                }
        }
        viewModelScope.launch {
            combine(settings.use12HourFormat, settings.geminiApiKey) { use12h, key -> use12h to key }
                .collect { (use12h, key) ->
                    _uiState.update { it.copy(use12h = use12h, apiKey = key, settingsLoaded = true) }
                }
        }
    }

    // ---- Form inputs ----

    fun onDistanceChange(value: String) = _uiState.update { it.copy(distanceInput = value) }

    fun onEconomyChange(value: String) = _uiState.update { it.copy(economyInput = value) }

    fun onTripNameChange(value: String) = _uiState.update { it.copy(tripNameInput = value) }

    fun onFuelPriceInputChange(value: String) = _uiState.update { it.copy(fuelPriceInput = value) }

    // ---- Actions ----

    /** OCR-first → Gemini-fallback scan of a dashboard photo. */
    fun processImage(bitmap: Bitmap?) {
        if (bitmap == null) return
        scanJob?.cancel()
        val apiKey = _uiState.value.apiKey
        scanJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                when (val outcome = scanPipeline.process(apiKey, bitmap)) {
                    is ScanOutcome.Success -> _uiState.update {
                        it.copy(
                            distanceInput = outcome.distanceKm,
                            economyInput = outcome.fuelEconomyKmL,
                            isLoading = false,
                            scanFeedback = true
                        )
                    }
                    is ScanOutcome.Failure -> _uiState.update {
                        it.copy(
                            errorMessage = outcome.message,
                            isLoading = false,
                            scanFeedback = true
                        )
                    }
                }
            } catch (e: CancellationException) {
                // Scan was cancelled from the progress card — clear the loading state.
                _uiState.update { it.copy(isLoading = false) }
                throw e
            }
        }
    }

    /** Cancels the in-flight scan; the progress card dismisses itself. */
    fun cancelScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isLoading = false) }
    }

    /** Validates and persists a trip; resets the form on success. */
    fun saveTrip() {
        val s = _uiState.value
        val dist = s.distanceInput.toDoubleOrNull() ?: 0.0
        val econ = s.economyInput.toDoubleOrNull() ?: 0.0
        val price = s.fuelPriceInput.toDoubleOrNull() ?: s.fuelPrice

        if (dist > 0.0 && econ > 0.0 && price > 0.0) {
            viewModelScope.launch {
                // Persist the price only when it actually changed, so logging a trip with the
                // same price doesn't bump the "Fuel Price as of" date.
                if (price != s.fuelPrice) {
                    settings.saveFuelPrice(price)
                }
                val newTrip = Trip(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    name = s.tripNameInput.takeIf { it.isNotBlank() },
                    distanceKm = dist,
                    fuelEconomyKmL = econ,
                    fuelPrice = price,
                    cost = s.calculatedCost
                )
                tripStore.saveTrip(newTrip)
                _uiState.update {
                    it.copy(
                        distanceInput = "",
                        economyInput = "",
                        tripNameInput = "",
                        fuelPriceInput = price.toString(),
                        errorMessage = null
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(errorMessage = "Please enter valid distance, fuel economy, and fuel price values.")
            }
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch { tripStore.deleteTrip(tripId) }
    }

    /** Persists the fuel price edited via the header dialog. */
    fun saveFuelPrice(price: Double) {
        viewModelScope.launch {
            settings.saveFuelPrice(price)
            _uiState.update { it.copy(fuelPriceInput = price.toString()) }
        }
    }

    fun showError(message: String) = _uiState.update { it.copy(errorMessage = message) }

    fun clearErrorMessage() = _uiState.update { it.copy(errorMessage = null) }

    fun consumeScanFeedback() = _uiState.update { it.copy(scanFeedback = false) }

    companion object {
        /** Factory wiring the production implementations. */
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    settings = SettingsRepository(context),
                    tripStore = TripRepository(context),
                    scanPipeline = DashboardScanPipeline(OcrManagerAdapter, GeminiDashboardParser)
                )
            }
        }
    }
}
