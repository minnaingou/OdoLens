package com.mndublo.odolens.ui.parking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mndublo.odolens.ui.common.ErrorCard
import com.mndublo.odolens.ui.common.MorphingParkingHeader
import com.mndublo.odolens.ui.common.ScanCameraOverlay
import com.mndublo.odolens.ui.common.loadBitmapFromUri
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(
    modifier: Modifier = Modifier,
    openExtendSheet: Boolean = false,
    onExtendSheetOpened: () -> Unit = {},
    autoScan: Boolean = false,
    onAutoScanHandled: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: ParkingViewModel = viewModel(
        factory = remember(context) { ParkingViewModel.factory(context.applicationContext) }
    )
    val uiState by viewModel.uiState.collectAsState()

    // Transient UI state (not persisted, owned by the screen)
    var showCamera by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showExtendSheet by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Open the extend sheet when the notification deep-link arrives
    LaunchedEffect(openExtendSheet) {
        if (openExtendSheet) {
            showExtendSheet = true
            onExtendSheetOpened()
        }
    }

    // Auto-open the camera when launched from a shortcut / notification
    LaunchedEffect(autoScan) {
        if (autoScan) {
            showCamera = true
            onAutoScanHandled()
        }
    }

    val listState = rememberLazyListState()

    // One-shot feedback from ViewModel actions
    LaunchedEffect(uiState.alarmJustScheduled) {
        if (uiState.alarmJustScheduled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            Toast.makeText(context, context.getString(com.mndublo.odolens.R.string.parking_alarm_set_toast), Toast.LENGTH_SHORT).show()
            viewModel.consumeAlarmJustScheduled()
        }
    }
    LaunchedEffect(uiState.scheduleFailed) {
        if (uiState.scheduleFailed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            listState.animateScrollToItem(0)
            viewModel.consumeScheduleFailed()
        }
    }
    LaunchedEffect(uiState.extendFailed) {
        if (uiState.extendFailed) {
            Toast.makeText(context, context.getString(com.mndublo.odolens.R.string.parking_extend_cap_toast), Toast.LENGTH_SHORT).show()
            viewModel.consumeExtendFailed()
        }
    }

    // Gallery Picker
    val galleryLoadErrorMessage = stringResource(com.mndublo.odolens.R.string.parking_gallery_load_error)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: android.net.Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    val bitmap = loadBitmapFromUri(context, uri)
                    if (bitmap != null) {
                        viewModel.parseTicket(bitmap)
                    } else {
                        viewModel.showError(galleryLoadErrorMessage)
                    }
                }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (isGranted) {
                viewModel.scheduleAlarm()
            } else {
                Toast.makeText(context, context.getString(com.mndublo.odolens.R.string.parking_notification_denied_toast), Toast.LENGTH_LONG).show()
            }
        }
    )

    // Scroll to top when timer starts or expires so the card is immediately visible
    LaunchedEffect(uiState.isTimerRunning, uiState.isExpired) {
        if (uiState.isTimerRunning || uiState.isExpired) {
            listState.animateScrollToItem(0)
        }
    }

    val showActiveOrExpiredCard = uiState.isTimerRunning || uiState.isExpired

    // Continuous scroll progress fraction for Idle Mode morphing header
    val collapseProgress by remember(showActiveOrExpiredCard) {
        derivedStateOf {
            if (showActiveOrExpiredCard) {
                0f
            } else if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 240f).coerceIn(0f, 1f)
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
                // Morphing Top Header (Only present in Idle Mode when timer is not active)
                if (!showActiveOrExpiredCard && !showCamera) {
                    MorphingParkingHeader(
                        progress = collapseProgress,
                        onCamera = { showCamera = true },
                        onGallery = { galleryLauncher.launch("image/*") },
                        isAiLoading = uiState.isAiLoading
                    )
                }

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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(com.mndublo.odolens.R.string.parking_no_api_key_warning),
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
                        item(key = "parkingError") {
                            ErrorCard(
                                errorMessage = error,
                                onDismiss = viewModel::clearErrorMessage,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    // Active Timer Mode (Hero countdown card)
                    if (showActiveOrExpiredCard) {
                        item(key = "activeTimerCard") {
                            ParkingTimerCard(
                                countdownText = uiState.countdownText,
                                calculatedExpiry = uiState.calculatedExpiry,
                                scheduledAlarmTime = uiState.scheduledAlarmTime,
                                parkingSpotNote = uiState.parkingSpotNoteInput,
                                progressFraction = uiState.timerProgressFraction,
                                isExpired = uiState.isExpired,
                                expiredAgoText = uiState.expiredAgoText,
                                onExtend = { showExtendSheet = true },
                                onReset = { viewModel.resetTimer() }
                            )
                        }
                    }

                    // Setup / Idle Mode Form Sections
                    if (!showActiveOrExpiredCard) {
                        item(key = "parkingFormSection") {
                            ParkingFormSection(
                                startTimeInput = uiState.startTimeInput,
                                use12h = uiState.use12h,
                                onQuickStart = viewModel::onQuickStart,
                                onPickTime = { showTimePicker = true },
                                freeDurationInput = uiState.freeDurationInput,
                                onFreeDurationChange = viewModel::onFreeDurationChange,
                                parkingSpotNoteInput = uiState.parkingSpotNoteInput,
                                onSpotNoteChange = viewModel::onSpotNoteChange,
                                calculatedExpiry = uiState.calculatedExpiry,
                                parkingDirectory = uiState.parkingDirectory,
                                selectedDirectoryEntryId = uiState.selectedDirectoryEntryId,
                                onDirectoryEntrySelected = viewModel::onDirectoryEntrySelected,
                                onOpenDirectory = viewModel::onOpenDirectorySheet
                            )
                        }
                    }

                    if (!showActiveOrExpiredCard) {
                        item(key = "alertOffsetSection") {
                            AlertOffsetSection(
                                warningOffsetMinutes = uiState.warningOffsetMinutes,
                                freeDurationMinutes = uiState.freeDurationInput.toIntOrNull() ?: 0,
                                isCustomOffsetSelected = uiState.isCustomOffsetSelected,
                                customOffsetInput = uiState.customOffsetInput,
                                onOffsetSelected = viewModel::onOffsetSelected,
                                onCustomOffsetSelected = viewModel::onCustomOffsetSelected,
                                onCustomOffsetInput = viewModel::onCustomOffsetInput,
                                onSetAlarm = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.scheduleAlarm()
                                    }
                                },
                                scheduledAlarmTime = uiState.scheduledAlarmTime
                            )
                        }
                    }
                }
            }

            // Camera Overlay
            ScanCameraOverlay(
                visible = showCamera,
                onBitmap = { viewModel.parseTicket(it) },
                onError = { viewModel.showError(it) },
                onClose = { showCamera = false }
            )

            // Extend Time Bottom Sheet
            if (showExtendSheet) {
                ExtendParkingSheet(
                    freeDurationMinutes = uiState.freeDurationMinutes,
                    onDismiss = { showExtendSheet = false },
                    onExtend = { viewModel.extendTimer(it) }
                )
            }

            // Parking Place Directory Sheet
            if (uiState.showDirectorySheet) {
                ParkingDirectorySheet(
                    directory = uiState.parkingDirectory,
                    selectedEntryId = uiState.selectedDirectoryEntryId,
                    onSelect = viewModel::onDirectoryEntrySelected,
                    onAdd = viewModel::onAddDirectoryEntry,
                    onEdit = viewModel::onEditDirectoryEntry,
                    onDelete = viewModel::onDeleteDirectoryEntry,
                    onDismiss = viewModel::onCloseDirectorySheet
                )
            }

            if (showTimePicker) {
                ParkingTimePickerDialog(
                    startTimeInput = uiState.startTimeInput,
                    use12h = uiState.use12h,
                    onTimePicked = viewModel::onTimePicked,
                    onDismiss = { showTimePicker = false }
                )
            }
        }
    }
}
