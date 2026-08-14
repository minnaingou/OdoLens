package com.mndublo.odolens.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mndublo.odolens.ui.common.ErrorCard
import com.mndublo.odolens.ui.common.MorphingDashboardHeader
import com.mndublo.odolens.ui.common.ScanCameraOverlay
import com.mndublo.odolens.ui.common.loadBitmapFromUri
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    autoScan: Boolean = false,
    onAutoScanHandled: () -> Unit = {},
    onViewAllTrips: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val viewModel: DashboardViewModel = viewModel(
        factory = remember(context) { DashboardViewModel.factory(context.applicationContext) }
    )
    val uiState by viewModel.uiState.collectAsState()

    // Transient UI state
    var showCamera by remember { mutableStateOf(false) }
    var showEditPriceDialog by remember { mutableStateOf(false) }
    var tripToEdit by remember { mutableStateOf<com.mndublo.odolens.data.Trip?>(null) }
    var editNameInput by remember { mutableStateOf("") }

    LaunchedEffect(autoScan) {
        if (autoScan) {
            showCamera = true
            onAutoScanHandled()
        }
    }

    // One-shot feedback from ViewModel actions (e.g., scan complete)
    LaunchedEffect(uiState.scanFeedback) {
        if (uiState.scanFeedback) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.consumeScanFeedback()
        }
    }

    // Gallery Picker
    val galleryLoadErrorMessage = stringResource(com.mndublo.odolens.R.string.dashboard_gallery_load_error)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: android.net.Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    val bitmap = loadBitmapFromUri(context, uri)
                    if (bitmap != null) {
                        viewModel.processImage(bitmap)
                    } else {
                        viewModel.showError(galleryLoadErrorMessage)
                    }
                }
            }
        }
    )

    val listState = rememberLazyListState()

    // Continuous scroll progress fraction (0f = expanded, 1f = collapsed into minibar)
    val collapseProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 280f).coerceIn(0f, 1f)
            }
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Morphing Top Header (Fuel Price + Hero Scan Actions)
                if (!showCamera) {
                    MorphingDashboardHeader(
                        progress = collapseProgress,
                        fuelPrice = uiState.fuelPrice,
                        fuelPriceDate = uiState.fuelPriceDate,
                        onEditPrice = { showEditPriceDialog = true },
                        onCamera = { showCamera = true },
                        onGallery = { galleryLauncher.launch("image/*") },
                        isLoading = uiState.isLoading,
                        statusMessage = uiState.statusMessage
                    )
                }

                // Main scrollable content
                LazyColumn(
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 88.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.settingsLoaded && uiState.apiKey.isBlank()) {
                        item(key = "apiKeyWarning") {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No Gemini API key set. Fuel economy reading may be inaccurate.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = { onNavigateToSettings() }
                                    ) {
                                        Text("Set up →", color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }
                    }

                    uiState.errorMessage?.let { error ->
                        item(key = "dashboardError") {
                            ErrorCard(
                                errorMessage = error,
                                onDismiss = viewModel::clearErrorMessage,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    // Form Fields Section
                    item(key = "tripDetailsForm") {
                        TripDetailsForm(
                            distanceInput = uiState.distanceInput,
                            onDistanceChange = viewModel::onDistanceChange,
                            economyInput = uiState.economyInput,
                            onEconomyChange = viewModel::onEconomyChange,
                            tripNameInput = uiState.tripNameInput,
                            onTripNameChange = viewModel::onTripNameChange,
                            fuelPriceInput = uiState.fuelPriceInput,
                            onFuelPriceChange = viewModel::onFuelPriceInputChange,
                            calculatedCost = uiState.calculatedCost,
                            onSave = { viewModel.saveTrip() }
                        )
                    }

                    // Historical Trips + swipe-to-delete list
                    tripList(
                        trips = uiState.trips,
                        use12h = uiState.use12h,
                        onDelete = { viewModel.deleteTrip(it) },
                        onViewAll = onViewAllTrips,
                        onEdit = { trip ->
                            tripToEdit = trip
                            editNameInput = trip.name ?: ""
                        }
                    )
                }
            }

            if (showEditPriceDialog) {
                EditFuelPriceDialog(
                    initialPrice = uiState.fuelPrice,
                    onSave = { price ->
                        viewModel.saveFuelPrice(price)
                        showEditPriceDialog = false
                    },
                    onDismiss = { showEditPriceDialog = false }
                )
            }

            if (tripToEdit != null) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { tripToEdit = null },
                    title = { Text("Edit Trip Name") },
                    text = {
                        androidx.compose.material3.OutlinedTextField(
                            value = editNameInput,
                            onValueChange = { editNameInput = it },
                            label = { Text("Trip Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val targetId = tripToEdit?.id
                                if (targetId != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    coroutineScope.launch {
                                        val repo = com.mndublo.odolens.data.TripRepository(context.applicationContext)
                                        repo.updateTripName(targetId, editNameInput)
                                    }
                                }
                                tripToEdit = null
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { tripToEdit = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Camera Overlay
            ScanCameraOverlay(
                visible = showCamera,
                onBitmap = { viewModel.processImage(it) },
                onError = { viewModel.showError(it) },
                onClose = { showCamera = false }
            )
        }
    }
}
