package com.mndublo.odolens.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = M3ExpressivePrimaryDark,
    onPrimary = M3ExpressiveOnPrimaryDark,
    primaryContainer = M3ExpressivePrimaryContainerDark,
    onPrimaryContainer = M3ExpressiveOnPrimaryContainerDark,
    secondary = M3ExpressiveSecondaryDark,
    onSecondary = M3ExpressiveOnSecondaryDark,
    secondaryContainer = M3ExpressiveSecondaryContainerDark,
    onSecondaryContainer = M3ExpressiveOnSecondaryContainerDark,
    tertiary = M3ExpressiveTertiaryDark,
    onTertiary = M3ExpressiveOnTertiaryDark,
    tertiaryContainer = M3ExpressiveTertiaryContainerDark,
    onTertiaryContainer = M3ExpressiveOnTertiaryContainerDark,
    background = M3ExpressiveBackgroundDark,
    onBackground = M3ExpressiveOnBackgroundDark,
    surface = M3ExpressiveSurfaceDark,
    onSurface = M3ExpressiveOnSurfaceDark,
    surfaceVariant = M3ExpressiveSurfaceVariantDark,
    onSurfaceVariant = M3ExpressiveOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = M3ExpressivePrimaryLight,
    onPrimary = M3ExpressiveOnPrimaryLight,
    primaryContainer = M3ExpressivePrimaryContainerLight,
    onPrimaryContainer = M3ExpressiveOnPrimaryContainerLight,
    secondary = M3ExpressiveSecondaryLight,
    onSecondary = M3ExpressiveOnSecondaryLight,
    secondaryContainer = M3ExpressiveSecondaryContainerLight,
    onSecondaryContainer = M3ExpressiveOnSecondaryContainerLight,
    tertiary = M3ExpressiveTertiaryLight,
    onTertiary = M3ExpressiveOnTertiaryLight,
    tertiaryContainer = M3ExpressiveTertiaryContainerLight,
    onTertiaryContainer = M3ExpressiveOnTertiaryContainerLight,
    background = M3ExpressiveBackgroundLight,
    onBackground = M3ExpressiveOnBackgroundLight,
    surface = M3ExpressiveSurfaceLight,
    onSurface = M3ExpressiveOnSurfaceLight,
    surfaceVariant = M3ExpressiveSurfaceVariantLight,
    onSurfaceVariant = M3ExpressiveOnSurfaceVariantLight
)

@Composable
fun TripAndTicketOCRTheme(
    themeMode: Int = 0, // 0 = System Default, 1 = Light Mode, 2 = Dark Mode
    dynamicColor: Boolean = false, // Wallpaper-derived palette (Material You, Android 12+)
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        typography = Typography,
        content = content
    )
}
