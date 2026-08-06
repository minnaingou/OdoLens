package com.mndublo.odolens.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mndublo.odolens.ui.common.ScanCameraOverlay
import com.mndublo.odolens.ui.common.loadBitmapFromUri
import com.mndublo.odolens.ui.common.rememberFabVisibility
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    autoScan: Boolean = false,
    onAutoScanHandled: () -> Unit = {},
    onViewAllTrips: () -> Unit = {}
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

    // One-shot feedback: haptic when a scan finishes (success or failure)
    LaunchedEffect(uiState.scanFeedback) {
        if (uiState.scanFeedback) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.consumeScanFeedback()
        }
    }

    // Picker launcher for Gallery
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
    val isFabVisible by rememberFabVisibility(listState)

    Scaffold(
        floatingActionButton = {
            if (!showCamera) {
                AnimatedVisibility(
                    visible = isFabVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showCamera = true },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera") },
                        text = { Text(stringResource(com.mndublo.odolens.R.string.dashboard_fab_scan)) },
                        shape = MaterialTheme.shapes.extraLarge,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 8.dp
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                // Header displaying active Fuel Price
                DashboardHeader(
                    fuelPrice = uiState.fuelPrice,
                    fuelPriceDate = uiState.fuelPriceDate,
                    onEditClick = { showEditPriceDialog = true }
                )

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

                // Main form and List of Trips
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                ) {
                    // OCR Scanning / Pickers Section
                    item {
                        TripScanCard(
                            onCamera = { showCamera = true },
                            onGallery = { galleryLauncher.launch("image/*") },
                            isLoading = uiState.isLoading,
                            statusMessage = uiState.statusMessage,
                            errorMessage = uiState.errorMessage
                        )
                    }

                    // Form Fields Section
                    item {
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
                        androidx.compose.material3.TextButton(
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
                        androidx.compose.material3.TextButton(onClick = { tripToEdit = null }) {
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
