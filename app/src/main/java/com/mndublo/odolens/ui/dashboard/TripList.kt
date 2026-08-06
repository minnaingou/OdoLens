package com.mndublo.odolens.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mndublo.odolens.data.Trip
import androidx.compose.animation.animateColorAsState

/**
 * LazyColumn section rendering the historical trips: header, empty state and
 * swipe-to-delete cards (EndToStart only, 50% threshold).
 */
fun LazyListScope.tripList(
    trips: List<Trip>,
    use12h: Boolean,
    onDelete: (tripId: String) -> Unit,
    onViewAll: (() -> Unit)? = null,
    onEdit: ((trip: Trip) -> Unit)? = null
) {
    // Historical Trips Header
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historical Trips",
                style = MaterialTheme.typography.titleLarge
            )
            if (onViewAll != null && trips.isNotEmpty()) {
                androidx.compose.material3.TextButton(onClick = onViewAll) {
                    Text("View All →", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // Empty Trips State
    if (trips.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved trips yet. Scan your dashboard to start!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    val displayTrips = if (onViewAll != null) trips.take(3) else trips

    items(displayTrips, key = { it.id }) { trip ->
        val haptic = LocalHapticFeedback.current

        @Suppress("DEPRECATION")
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                when (dismissValue) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        onDelete(trip.id)
                        true
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (onEdit != null) {
                            onEdit(trip)
                        }
                        false // Don't dismiss item out of list when editing
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
            modifier = Modifier.padding(vertical = 4.dp),
            enableDismissFromStartToEnd = onEdit != null,
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
                TripCard(trip = trip, use12h = use12h)
            }
        )
    }
}
