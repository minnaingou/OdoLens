package com.mndublo.odolens.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.mndublo.odolens.AllTrips
import com.mndublo.odolens.ui.dashboard.DashboardScreen
import com.mndublo.odolens.ui.parking.ParkingScreen
import com.mndublo.odolens.ui.settings.SettingsScreen

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    openExtendSheet: Boolean = false,
    onExtendSheetOpened: () -> Unit = {},
    openParkingTab: Boolean = false,
    onParkingTabOpened: () -> Unit = {},
    openSettingsTab: Boolean = false,
    onSettingsTabOpened: () -> Unit = {},
    autoScanTarget: String? = null,
    onAutoScanHandled: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var scrollToApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(openExtendSheet, openParkingTab, openSettingsTab, autoScanTarget) {
        if (openSettingsTab) {
            selectedTab = 2
            scrollToApiKey = true
            onSettingsTabOpened()
        } else if (openParkingTab || openExtendSheet) {
            selectedTab = 1
            onParkingTabOpened()
        } else if (autoScanTarget == "trips") {
            selectedTab = 0
        } else if (autoScanTarget == "parking") {
            selectedTab = 1
        }
    }

    Scaffold(
        bottomBar = {
            // Floating pill navigation bar — the signature M3 Expressive look.
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 6.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 10.dp)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0.dp)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Trips") },
                        label = { Text(stringResource(com.mndublo.odolens.R.string.tab_trips), style = MaterialTheme.typography.labelMedium) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Timer, contentDescription = "Parking") },
                        label = { Text(stringResource(com.mndublo.odolens.R.string.tab_parking), style = MaterialTheme.typography.labelMedium) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text(stringResource(com.mndublo.odolens.R.string.tab_settings), style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Springy slide+fade between tabs so the expressive motion is felt.
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 20 })
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    0 -> DashboardScreen(
                        autoScan = autoScanTarget == "trips",
                        onAutoScanHandled = onAutoScanHandled,
                        onViewAllTrips = { onItemClick(AllTrips) },
                        onNavigateToSettings = {
                            selectedTab = 2
                            scrollToApiKey = true
                        }
                    )
                    1 -> ParkingScreen(
                        openExtendSheet = openExtendSheet,
                        onExtendSheetOpened = onExtendSheetOpened,
                        autoScan = autoScanTarget == "parking",
                        onAutoScanHandled = onAutoScanHandled
                    )
                    2 -> SettingsScreen(
                        scrollToApiKey = scrollToApiKey,
                        onScrollToApiKeyHandled = { scrollToApiKey = false }
                    )
                }
            }
        }
    }
}
