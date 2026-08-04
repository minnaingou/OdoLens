package com.mndublo.odolens

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

// First-launch notification permission explainer screen
@Serializable data object Permission : NavKey
