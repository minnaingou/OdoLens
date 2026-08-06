package com.mndublo.odolens

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

// First-launch notification permission explainer screen
@Serializable data object Permission : NavKey

// First-launch Gemini API key setup explainer screen
@Serializable data object ApiKeySetup : NavKey

// All trips list with search and edit
@Serializable data object AllTrips : NavKey
