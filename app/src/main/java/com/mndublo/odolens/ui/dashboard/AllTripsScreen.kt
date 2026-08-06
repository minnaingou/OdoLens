package com.mndublo.odolens.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mndublo.odolens.data.SettingsRepository
import com.mndublo.odolens.data.TimeFormatter
import com.mndublo.odolens.data.Trip
import com.mndublo.odolens.data.TripRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTripsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val tripRepository = remember(context) { TripRepository(context.applicationContext) }
    val settingsRepository = remember(context) { SettingsRepository(context.applicationContext) }

    val trips by tripRepository.trips.collectAsState(initial = emptyList())
    val use12h by settingsRepository.use12HourFormat.collectAsState(initial = false)

    var searchQuery by remember { mutableStateOf("") }
    var tripToEdit by remember { mutableStateOf<Trip?>(null) }
    var editNameInput by remember { mutableStateOf("") }

    val filteredTrips = remember(trips, searchQuery, use12h) {
        if (searchQuery.isBlank()) {
            trips
        } else {
            trips.filter { trip ->
                val dateStr = TimeFormatter.formatTimestamp(trip.timestamp, use12h)
                val displayName = trip.name ?: "Trip on $dateStr"
                displayName.contains(searchQuery, ignoreCase = true) || dateStr.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Historical Trips") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Filter by trip name or date") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredTrips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No saved trips yet." else "No trips matching '$searchQuery'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTrips, key = { it.id }) { trip ->
                        @Suppress("DEPRECATION")
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        coroutineScope.launch {
                                            tripRepository.deleteTrip(trip.id)
                                        }
                                        true
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        tripToEdit = trip
                                        editNameInput = trip.name ?: ""
                                        false
                                    }
                                    else -> false
                                }
                            },
                            positionalThreshold = { totalDistance -> totalDistance * 0.5f }
                        )

                        // Haptic feedback right as the swipe passes the 50% threshold in either direction
                        LaunchedEffect(dismissState.targetValue) {
                            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier.padding(vertical = 2.dp),
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val isThresholdReached = dismissState.targetValue != SwipeToDismissBoxValue.Settled
                                val isStartToEnd = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd || dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd

                                val backgroundColor by animateColorAsState(
                                    when {
                                        isStartToEnd && isThresholdReached -> MaterialTheme.colorScheme.primary
                                        isStartToEnd -> MaterialTheme.colorScheme.primaryContainer
                                        isThresholdReached -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                    },
                                    label = "backgroundColor"
                                )
                                val iconScale by animateFloatAsState(
                                    if (isThresholdReached) 1.3f else 1.0f,
                                    label = "iconScale"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(backgroundColor, shape = MaterialTheme.shapes.large)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                    ) {
                                        if (isStartToEnd) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Trip",
                                                tint = if (isThresholdReached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            if (isThresholdReached) {
                                                Text(
                                                    text = "Release to Edit",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Trip",
                                                tint = if (isThresholdReached) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            if (isThresholdReached) {
                                                Text(
                                                    text = "Release to Delete",
                                                    color = MaterialTheme.colorScheme.onError,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            content = {
                                TripCard(
                                    trip = trip,
                                    use12h = use12h
                                )
                            }
                        )
                    }
                }
            }
        }

        // Edit Trip Name Dialog
        if (tripToEdit != null) {
            AlertDialog(
                onDismissRequest = { tripToEdit = null },
                title = { Text("Edit Trip Name") },
                text = {
                    OutlinedTextField(
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
                                    tripRepository.updateTripName(targetId, editNameInput)
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
    }
}
