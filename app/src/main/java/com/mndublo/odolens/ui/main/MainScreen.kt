package com.mndublo.odolens.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
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
    autoScanTarget: String? = null,
    onAutoScanHandled: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(openExtendSheet, openParkingTab, autoScanTarget) {
        if (openParkingTab || openExtendSheet) {
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 6.dp
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
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    autoScan = autoScanTarget == "trips",
                    onAutoScanHandled = onAutoScanHandled
                )
                1 -> ParkingScreen(
                    openExtendSheet = openExtendSheet,
                    onExtendSheetOpened = onExtendSheetOpened,
                    autoScan = autoScanTarget == "parking",
                    onAutoScanHandled = onAutoScanHandled
                )
                2 -> SettingsScreen()
            }
        }
    }
}
