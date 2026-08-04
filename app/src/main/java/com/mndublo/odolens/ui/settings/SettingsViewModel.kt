package com.mndublo.odolens.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mndublo.odolens.data.SettingsRepository
import com.mndublo.odolens.data.SettingsSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKeyInput: String = "",
    val use12hFormat: Boolean = false,
    val themeMode: Int = 0,
    // One-shot UI feedback flag, consumed by the screen (save toast)
    val saveFeedback: Boolean = false
)

class SettingsViewModel(
    private val settings: SettingsSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Sync inputs from persisted values (same behavior as the old LaunchedEffect)
        viewModelScope.launch {
            combine(settings.geminiApiKey, settings.use12HourFormat, settings.themeMode) { key, use12h, theme ->
                Triple(key, use12h, theme)
            }.collect { (key, use12h, theme) ->
                _uiState.update {
                    it.copy(apiKeyInput = key, use12hFormat = use12h, themeMode = theme)
                }
            }
        }
    }

    fun onApiKeyChange(value: String) = _uiState.update { it.copy(apiKeyInput = value) }

    /** Persists immediately, matching the old chip behavior. */
    fun onUse12hChange(value: Boolean) {
        _uiState.update { it.copy(use12hFormat = value) }
        viewModelScope.launch { settings.saveUse12HourFormat(value) }
    }

    /** Persists immediately, matching the old chip behavior. */
    fun onThemeModeChange(value: Int) {
        _uiState.update { it.copy(themeMode = value) }
        viewModelScope.launch { settings.saveThemeMode(value) }
    }

    /** Saves all settings and signals the screen to show the confirmation toast. */
    fun saveAll() {
        val s = _uiState.value
        viewModelScope.launch {
            settings.saveGeminiApiKey(s.apiKeyInput)
            settings.saveUse12HourFormat(s.use12hFormat)
            settings.saveThemeMode(s.themeMode)
            _uiState.update { it.copy(saveFeedback = true) }
        }
    }

    fun consumeSaveFeedback() = _uiState.update { it.copy(saveFeedback = false) }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(SettingsRepository(context)) }
        }
    }
}
