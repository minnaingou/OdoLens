package com.mndublo.odolens.ui.parking

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mndublo.odolens.data.ParkingPlace
import com.mndublo.odolens.data.ParkingSettingsSource
import com.mndublo.odolens.data.ParkingTimerPlanner
import com.mndublo.odolens.data.SettingsRepository
import com.mndublo.odolens.data.TimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ParkingUiState(
    val apiKey: String = "",
    // True once persisted settings have been read; gates the missing-key warning so it
    // doesn't flash on launch before the real value arrives.
    val settingsLoaded: Boolean = false,
    val use12h: Boolean = false,
    val expiryMs: Long = 0L,
    val alarmTime: String = "",
    val spotNote: String = "",
    val startTime: String = "",
    val freeDurationMinutes: Int = 0,
    val savedOffsetMinutes: Int = 60,
    val startTimeInput: String = "00:00",
    val freeDurationInput: String = "0",
    val parkingSpotNoteInput: String = "",
    val warningOffsetMinutes: Int = 60,
    val customOffsetInput: String = "",
    val isCustomOffsetSelected: Boolean = false,
    val isAiLoading: Boolean = false,
    val errorMessage: String? = null,
    val countdownText: String = "",
    /** Remaining fraction of the free-parking window (1.0 → 0.0), refreshed by the countdown ticker. */
    val timerProgressFraction: Float = 1f,
    /** True when the active parking session has expired and is waiting for user acknowledgment. */
    val isExpired: Boolean = false,
    /** Elapsed time since expiration: e.g. "5m 20s" */
    val expiredAgoText: String = "",
    // One-shot UI feedback flags, consumed by the screen
    val alarmJustScheduled: Boolean = false,
    val scheduleFailed: Boolean = false,
    val extendFailed: Boolean = false,
    // Parking place directory
    val parkingDirectory: List<ParkingPlace> = emptyList(),
    val selectedDirectoryEntryId: String? = null,
    val showDirectorySheet: Boolean = false
) {
    val isTimerRunning: Boolean get() = expiryMs > 0L && !isExpired
    val scheduledAlarmTime: String? get() = alarmTime.ifBlank { null }

    /** Expiry shown in the timer card / form preview. */
    val calculatedExpiry: String
        get() = if (expiryMs > 0L) {
            val cal = Calendar.getInstance().apply { timeInMillis = expiryMs }
            TimeFormatter.formatTime(
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                use12h
            )
        } else {
            ParkingTimerPlanner.formatExpiryDisplay(
                startTimeInput,
                freeDurationInput.toIntOrNull() ?: 0,
                use12h
            )
        }
}

class ParkingViewModel(
    private val settings: ParkingSettingsSource,
    private val scheduler: ParkingAlarmScheduler,
    private val parser: ParkingTicketParser,
    private val now: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParkingUiState())
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        // Merge persisted settings into the state, re-deriving offset-UI fields the way the
        // old LaunchedEffect(savedOffsetMinutes) did, and seeding the form from a persisted
        // timer so it survives tab switches.
        viewModelScope.launch {
            combine(settings.geminiApiKey, settings.use12HourFormat) { apiKey, use12h -> apiKey to use12h }
                .collect { (apiKey, use12h) ->
                    _uiState.update { it.copy(apiKey = apiKey, use12h = use12h, settingsLoaded = true) }
                }
        }
        viewModelScope.launch {
            combine(settings.parkingAlarmTime, settings.parkingFreeDuration) { alarm, free -> alarm to free }
                .collect { (alarm, free) ->
                    _uiState.update { it.copy(alarmTime = alarm, freeDurationMinutes = free) }
                }
        }
        viewModelScope.launch {
            combine(
                settings.parkingExpiryMs,
                settings.parkingStartTime,
                settings.parkingSpotNote,
                settings.parkingIsExpired
            ) { expiry, start, spot, persistedExpired ->
                val nowMs = now()
                // Auto-purge stale expired session (e.g. past midnight / next calendar day)
                if (expiry > 0L && ParkingTimerPlanner.isStaleExpired(expiry, nowMs)) {
                    resetTimer()
                    return@combine
                }

                val hasExpired = (expiry > 0L && nowMs >= expiry) || persistedExpired
                val expiredAgo = if (hasExpired && expiry > 0L) {
                    ParkingTimerPlanner.formatExpiredDuration(nowMs - expiry)
                } else {
                    ""
                }

                _uiState.update { st ->
                    st.copy(
                        expiryMs = expiry,
                        startTime = start,
                        spotNote = spot,
                        isExpired = hasExpired,
                        expiredAgoText = expiredAgo,
                        startTimeInput = if (expiry > 0L && start.isNotBlank()) start else st.startTimeInput,
                        parkingSpotNoteInput = if (expiry > 0L && spot.isNotBlank()) spot else st.parkingSpotNoteInput
                    )
                }
            }.collect {}
        }
        viewModelScope.launch {
            settings.notificationOffsetMinutes.collect { offset ->
                val isCustom = offset != 15 && offset != 30 && offset != 45 && offset != 60
                _uiState.update { st ->
                    st.copy(
                        savedOffsetMinutes = offset,
                        warningOffsetMinutes = offset,
                        isCustomOffsetSelected = isCustom,
                        customOffsetInput = if (isCustom) offset.toString() else st.customOffsetInput
                    )
                }
            }
        }

        // Live countdown / expired ticker while a timer or expired session is active.
        viewModelScope.launch {
            _uiState.map { Triple(it.expiryMs, it.isExpired, it.freeDurationMinutes) }
                .distinctUntilChanged()
                .collect { (expiry, isExpired, _) ->
                    countdownJob?.cancel()
                    if (expiry > 0L) {
                        countdownJob = viewModelScope.launch {
                            while (isActive) {
                                val nowMs = now()
                                if (ParkingTimerPlanner.isStaleExpired(expiry, nowMs)) {
                                    resetTimer()
                                    break
                                }

                                val hasExpired = isExpired || nowMs >= expiry
                                if (hasExpired) {
                                    val expiredAgo = ParkingTimerPlanner.formatExpiredDuration(nowMs - expiry)
                                    _uiState.update { st ->
                                        st.copy(
                                            isExpired = true,
                                            expiredAgoText = expiredAgo,
                                            countdownText = "Expired!",
                                            timerProgressFraction = 0f
                                        )
                                    }
                                } else {
                                    _uiState.update { st ->
                                        st.copy(
                                            isExpired = false,
                                            countdownText = ParkingTimerPlanner.formatCountdown(expiry - nowMs),
                                            timerProgressFraction = ParkingTimerPlanner.remainingFraction(
                                                expiryMs = expiry,
                                                totalMs = ParkingTimerPlanner.countdownTotalMs(
                                                    freeDurationMinutes = st.freeDurationMinutes,
                                                    startTime = st.startTime,
                                                    expiryMs = expiry
                                                ),
                                                nowMs = nowMs
                                            )
                                        )
                                    }
                                }
                                delay(1000)
                            }
                        }
                    } else {
                        _uiState.update { it.copy(countdownText = "", timerProgressFraction = 1f, isExpired = false, expiredAgoText = "") }
                    }
                }
        }

        // Parking place directory.
        viewModelScope.launch {
            settings.parkingPlaceDirectory.collect { places ->
                _uiState.update { it.copy(parkingDirectory = places) }
            }
        }
    }

    // ---- Form inputs ----

    fun onStartTimeChange(value: String) = _uiState.update { it.copy(startTimeInput = value) }

    fun onFreeDurationChange(value: String) {
        val selectedEntry = _uiState.value.let { st ->
            st.selectedDirectoryEntryId?.let { id -> st.parkingDirectory.find { it.id == id } }
        }
        // Clear directory selection badge when the user drags to a different value
        val clearSelection = selectedEntry != null && value != selectedEntry.freeMinutes.toString()
        _uiState.update {
            it.copy(
                freeDurationInput = value,
                selectedDirectoryEntryId = if (clearSelection) null else it.selectedDirectoryEntryId
            )
        }
    }

    fun onSpotNoteChange(value: String) = _uiState.update { it.copy(parkingSpotNoteInput = value) }

    /** Quick preset: set start time to "minutesAgo" minutes before now. */
    fun onQuickStart(minutesAgo: Int) {
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, -minutesAgo) }
        _uiState.update {
            it.copy(
                startTimeInput = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE)
                )
            )
        }
    }

    fun onTimePicked(hour: Int, minute: Int) {
        _uiState.update {
            it.copy(
                startTimeInput = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            )
        }
    }

    // ---- Warning offset ----

    fun onOffsetSelected(minutes: Int) {
        _uiState.update { it.copy(isCustomOffsetSelected = false, warningOffsetMinutes = minutes) }
        viewModelScope.launch { settings.saveNotificationOffsetMinutes(minutes) }
    }

    /** User tapped the "Custom Minutes" chip — reveal the custom input field. */
    fun onCustomOffsetSelected() {
        _uiState.update { it.copy(isCustomOffsetSelected = true) }
    }

    fun onCustomOffsetInput(value: String) {
        val mins = value.toIntOrNull() ?: 0
        _uiState.update { it.copy(customOffsetInput = value, warningOffsetMinutes = mins) }
        if (mins > 0) {
            viewModelScope.launch { settings.saveNotificationOffsetMinutes(mins) }
        }
    }

    // ---- Actions ----

    fun scheduleAlarm() {
        val s = _uiState.value
        val freeDur = s.freeDurationInput.toIntOrNull() ?: 0
        // Reject schedules whose free window has already elapsed (or that are malformed, or offset >= duration) —
        // never silently roll a past expiry to tomorrow.
        val validationError = ParkingTimerPlanner.validateSchedule(
            startTime = s.startTimeInput,
            freeMinutes = freeDur,
            offsetMinutes = s.warningOffsetMinutes,
            now = now()
        )
        if (validationError != null) {
            _uiState.update {
                it.copy(errorMessage = validationError, scheduleFailed = true)
            }
            return
        }
        viewModelScope.launch {
            try {
                val alarmMs = scheduler.scheduleParkingAlarm(
                    startTime = s.startTimeInput,
                    freeDurationMinutes = freeDur,
                    offsetMinutes = s.warningOffsetMinutes,
                    spotNote = s.parkingSpotNoteInput,
                    use12h = s.use12h
                )
                val expiry = ParkingTimerPlanner.computeExpiryMillis(s.startTimeInput, freeDur)
                val alarmStr = TimeFormatter.formatAlarmTime(alarmMs, s.use12h)
                scheduler.scheduleExpiryAlarm(expiry, s.parkingSpotNoteInput, s.use12h)
                settings.saveParkingTimer(
                    expiryMs = expiry,
                    alarmTime = alarmStr,
                    spotNote = s.parkingSpotNoteInput,
                    startTime = s.startTimeInput,
                    freeDurationMinutes = freeDur,
                    offsetMinutes = s.warningOffsetMinutes
                )
                _uiState.update { it.copy(errorMessage = null, alarmJustScheduled = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Failed to schedule alarm: ${e.message}",
                        scheduleFailed = true
                    )
                }
            }
        }
    }

    fun resetTimer() {
        viewModelScope.launch {
            scheduler.cancelParkingAlarm()
            scheduler.cancelExpiryAlarm()
            settings.clearParkingTimer()
            _uiState.update {
                it.copy(
                    countdownText = "",
                    timerProgressFraction = 1f,
                    isExpired = false,
                    expiredAgoText = "",
                    expiryMs = 0L,
                    alarmTime = "",
                    spotNote = "",
                    startTime = "",
                    freeDurationMinutes = 0,
                    startTimeInput = "00:00",
                    freeDurationInput = "0",
                    parkingSpotNoteInput = "",
                    errorMessage = null
                )
            }
        }
    }

    fun extendTimer(hours: Int) {
        viewModelScope.launch {
            val extended = scheduler.extendTimer(hours * 60)
            if (!extended) {
                _uiState.update { it.copy(extendFailed = true) }
            }
        }
    }

    /**
     * AI-parse a ticket photo and fill the start-time / free-duration fields.
     * The bitmap is only forwarded to the parser (never inspected here); nullable so the
     * orchestration stays unit-testable on the JVM.
     */
    fun parseTicket(bitmap: Bitmap?) {
        val apiKey = _uiState.value.apiKey
        _uiState.update { it.copy(isAiLoading = true) }
        viewModelScope.launch {
            val result = parser.parse(apiKey, bitmap)
            _uiState.update { it.copy(isAiLoading = false) }
            result.fold(
                onSuccess = { data ->
                    _uiState.update {
                        it.copy(
                            startTimeInput = data.startTime,
                            freeDurationInput = data.freeDurationMinutes.toString()
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(errorMessage = "AI Parse failed: ${err.message}") }
                }
            )
        }
    }

    fun showError(message: String) = _uiState.update { it.copy(errorMessage = message) }

    fun clearErrorMessage() = _uiState.update { it.copy(errorMessage = null) }

    // ---- One-shot feedback consumption ----

    fun consumeAlarmJustScheduled() = _uiState.update { it.copy(alarmJustScheduled = false) }

    fun consumeScheduleFailed() = _uiState.update { it.copy(scheduleFailed = false) }

    fun consumeExtendFailed() = _uiState.update { it.copy(extendFailed = false) }

    // ---- Parking Place Directory ----

    /** Select a directory entry: pre-fill free duration, mark active, and update last used timestamp. */
    fun onDirectoryEntrySelected(place: ParkingPlace) {
        val currentNow = now()
        _uiState.update {
            it.copy(
                freeDurationInput = place.freeMinutes.toString(),
                selectedDirectoryEntryId = place.id
            )
        }
        val updated = _uiState.value.parkingDirectory
            .map { if (it.id == place.id) it.copy(lastUsedEpochMs = currentNow) else it }
        viewModelScope.launch { settings.saveParkingPlaceDirectory(updated) }
    }

    fun onOpenDirectorySheet() = _uiState.update { it.copy(showDirectorySheet = true) }

    fun onCloseDirectorySheet() = _uiState.update { it.copy(showDirectorySheet = false) }

    /** Add a new named place to the directory and persist. */
    fun onAddDirectoryEntry(name: String, freeMinutes: Int) {
        val newPlace = ParkingPlace(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            freeMinutes = freeMinutes,
            lastUsedEpochMs = now()
        )
        val updated = (_uiState.value.parkingDirectory + newPlace)
        viewModelScope.launch { settings.saveParkingPlaceDirectory(updated) }
    }

    /** Edit an existing entry by id and persist. */
    fun onEditDirectoryEntry(id: String, name: String, freeMinutes: Int) {
        val updated = _uiState.value.parkingDirectory
            .map { if (it.id == id) it.copy(name = name.trim(), freeMinutes = freeMinutes) else it }
        viewModelScope.launch { settings.saveParkingPlaceDirectory(updated) }
    }

    /** Delete an entry by id; clears selection if the deleted entry was active. */
    fun onDeleteDirectoryEntry(id: String) {
        val updated = _uiState.value.parkingDirectory.filter { it.id != id }
        _uiState.update {
            it.copy(selectedDirectoryEntryId = if (it.selectedDirectoryEntryId == id) null else it.selectedDirectoryEntryId)
        }
        viewModelScope.launch { settings.saveParkingPlaceDirectory(updated) }
    }

    companion object {
        /** Factory wiring the production implementations. */
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val settings = SettingsRepository(context)
                ParkingViewModel(
                    settings = settings,
                    scheduler = NotificationParkingScheduler(context, settings),
                    parser = GeminiParkingTicketParser
                )
            }
        }
    }
}
