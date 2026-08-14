package com.mndublo.odolens.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) : ParkingSettingsSource, DashboardSettingsSource, SettingsSource {
    companion object {
        private val FUEL_PRICE = doublePreferencesKey("fuel_price")
        private val FUEL_PRICE_DATE = stringPreferencesKey("fuel_price_date")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val NOTIFICATION_OFFSET_MINUTES = intPreferencesKey("notification_offset_minutes")
        private val USE_12_HOUR_FORMAT = androidx.datastore.preferences.core.booleanPreferencesKey("use_12_hour_format")
        private val THEME_MODE = intPreferencesKey("theme_mode") // 0 = System, 1 = Light, 2 = Dark
        private val DYNAMIC_COLOR = androidx.datastore.preferences.core.booleanPreferencesKey("dynamic_color")
        // Parking timer persistence — survives tab switches
        private val PARKING_EXPIRY_MS = longPreferencesKey("parking_expiry_ms")
        private val PARKING_ALARM_TIME = stringPreferencesKey("parking_alarm_time")
        private val PARKING_SPOT_NOTE = stringPreferencesKey("parking_spot_note")
        private val PARKING_START_TIME = stringPreferencesKey("parking_start_time")
        private val PARKING_FREE_DURATION = intPreferencesKey("parking_free_duration")
        private val PARKING_OFFSET_MINUTES = intPreferencesKey("parking_offset_minutes_snapshot")
        // First-launch notification permission prompt — shown only once
        private val NOTIFICATION_PROMPT_DONE = androidx.datastore.preferences.core.booleanPreferencesKey("notification_prompt_done")
        // Parking place directory — user-managed list of named places with free hours
        private val PARKING_PLACE_DIRECTORY = stringPreferencesKey("parking_place_directory")
    }

    override val fuelPrice: Flow<Double> = context.settingsDataStore.data.map { preferences ->
        preferences[FUEL_PRICE] ?: 35.0 // Default price
    }

    override val fuelPriceDate: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[FUEL_PRICE_DATE] ?: ""
    }

    override val geminiApiKey: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY] ?: "" // No default key — user must add their own in Settings
    }

    override val notificationOffsetMinutes: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[NOTIFICATION_OFFSET_MINUTES] ?: 60 // Default 1 hour
    }

    override val use12HourFormat: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[USE_12_HOUR_FORMAT] ?: false // Default 24-hour
    }

    override val themeMode: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: 0 // 0 = System Default, 1 = Light, 2 = Dark
    }

    override val dynamicColor: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR] ?: false // Off by default — app keeps its branded palette
    }

    // Parking timer state — 0L means no active timer
    override val parkingExpiryMs: Flow<Long> = context.settingsDataStore.data.map { preferences ->
        preferences[PARKING_EXPIRY_MS] ?: 0L
    }

    override val parkingAlarmTime: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[PARKING_ALARM_TIME] ?: ""
    }

    override val parkingSpotNote: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[PARKING_SPOT_NOTE] ?: ""
    }

    override val parkingStartTime: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[PARKING_START_TIME] ?: ""
    }

    override val parkingFreeDuration: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[PARKING_FREE_DURATION] ?: 0
    }

    override val parkingOffsetMinutes: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[PARKING_OFFSET_MINUTES] ?: 60
    }

    override val parkingPlaceDirectory: Flow<List<ParkingPlace>> = context.settingsDataStore.data.map { preferences ->
        ParkingPlaceSerializer.decode(preferences[PARKING_PLACE_DIRECTORY] ?: "")
    }

    // Whether the first-launch notification permission prompt has been shown
    val notificationPromptDone: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[NOTIFICATION_PROMPT_DONE] ?: false
    }

    override suspend fun saveFuelPrice(price: Double) {
        val currentDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        context.settingsDataStore.edit { preferences ->
            val current = preferences[FUEL_PRICE]
            preferences[FUEL_PRICE] = price
            // Only move the "as of" date when the price actually changed
            // (or there was no stored price yet), so logging trips with the
            // same price no longer bumps the date.
            if (current == null || current != price) {
                preferences[FUEL_PRICE_DATE] = currentDate
            }
        }
    }

    suspend fun saveFuelPriceWithDate(price: Double, date: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[FUEL_PRICE] = price
            preferences[FUEL_PRICE_DATE] = date
        }
    }

    override suspend fun saveGeminiApiKey(key: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[GEMINI_API_KEY] = key
        }
    }

    override suspend fun saveNotificationOffsetMinutes(minutes: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTIFICATION_OFFSET_MINUTES] = minutes
        }
    }

    override suspend fun saveUse12HourFormat(use12h: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[USE_12_HOUR_FORMAT] = use12h
        }
    }

    override suspend fun saveThemeMode(mode: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    override suspend fun saveDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setNotificationPromptDone(done: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTIFICATION_PROMPT_DONE] = done
        }
    }

    override suspend fun saveParkingPlaceDirectory(places: List<ParkingPlace>) {
        context.settingsDataStore.edit { preferences ->
            preferences[PARKING_PLACE_DIRECTORY] = ParkingPlaceSerializer.encode(places)
        }
    }

    override suspend fun saveParkingTimer(
        expiryMs: Long,
        alarmTime: String,
        spotNote: String,
        startTime: String,
        freeDurationMinutes: Int,
        offsetMinutes: Int
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[PARKING_EXPIRY_MS] = expiryMs
            preferences[PARKING_ALARM_TIME] = alarmTime
            preferences[PARKING_SPOT_NOTE] = spotNote
            preferences[PARKING_START_TIME] = startTime
            preferences[PARKING_FREE_DURATION] = freeDurationMinutes
            preferences[PARKING_OFFSET_MINUTES] = offsetMinutes
        }
    }

    /** Atomically clears the persisted parking timer state. */
    override suspend fun clearParkingTimer() {
        context.settingsDataStore.edit { preferences ->
            preferences[PARKING_EXPIRY_MS] = 0L
            preferences[PARKING_ALARM_TIME] = ""
            preferences[PARKING_SPOT_NOTE] = ""
            preferences[PARKING_START_TIME] = ""
            preferences[PARKING_FREE_DURATION] = 0
            preferences[PARKING_OFFSET_MINUTES] = 60
        }
    }
}
