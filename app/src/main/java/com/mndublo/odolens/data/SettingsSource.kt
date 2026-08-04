package com.mndublo.odolens.data

import kotlinx.coroutines.flow.Flow

/** Settings-screen view of the persisted preferences (unit-testable seam). */
interface SettingsSource {
    val geminiApiKey: Flow<String>
    val use12HourFormat: Flow<Boolean>
    val themeMode: Flow<Int> // 0 = System, 1 = Light, 2 = Dark

    suspend fun saveGeminiApiKey(key: String)
    suspend fun saveUse12HourFormat(use12h: Boolean)
    suspend fun saveThemeMode(mode: Int)
}
