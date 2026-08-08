package com.mndublo.odolens.ui.parking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mndublo.odolens.data.TimeFormatter
import java.util.Locale

/** "Expiration Details" form: quick-start presets, start time, free-duration slider, spot note, expiry preview. */
@Composable
fun ParkingFormSection(
    startTimeInput: String,
    use12h: Boolean,
    onQuickStart: (minutesAgo: Int) -> Unit,
    onPickTime: () -> Unit,
    freeDurationInput: String,
    onFreeDurationChange: (String) -> Unit,
    parkingSpotNoteInput: String,
    onSpotNoteChange: (String) -> Unit,
    calculatedExpiry: String
) {
    val haptic = LocalHapticFeedback.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Expiration Details",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = TimeFormatter.formatStartTime(startTimeInput, use12h),
                onValueChange = {},
                readOnly = true,
                label = { Text("Parking Start Time") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onPickTime) {
                        Icon(Icons.Default.Edit, contentDescription = "Pick Time")
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        "Now" to 0,
                        "5m ago" to 5,
                        "10m ago" to 10,
                        "15m ago" to 15,
                        "30m ago" to 30
                    )
                ) { (label, mins) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onQuickStart(mins)
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            val currentMinutes = freeDurationInput.toIntOrNull() ?: 0
            val currentHours = (currentMinutes / 60f).coerceIn(0f, 8f)

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Free Duration",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    val displayStr = if (currentMinutes % 60 == 0) {
                        "${currentMinutes / 60} ${if (currentMinutes / 60 == 1) "Hour" else "Hours"}"
                    } else {
                        String.format(Locale.getDefault(), "%.1f Hours (%d mins)", currentHours, currentMinutes)
                    }
                    Text(
                        text = displayStr,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                var lastSliderStep by remember { mutableIntStateOf(-1) }
                Slider(
                    value = currentHours,
                    onValueChange = { hours ->
                        val step = (hours * 2).toInt()
                        if (step != lastSliderStep) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastSliderStep = step
                        }
                        val mins = (hours * 60).toInt()
                        onFreeDurationChange(mins.toString())
                    },
                    valueRange = 0f..8f,
                    steps = 15,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = parkingSpotNoteInput,
                onValueChange = onSpotNoteChange,
                label = { Text("Parking Spot / Note (e.g. 2F 21)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expires at:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = calculatedExpiry,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

/** "Alert Warning Offset" card: preset/custom offset chips and the Set Alarm Reminder button. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlertOffsetSection(
    warningOffsetMinutes: Int,
    isCustomOffsetSelected: Boolean,
    customOffsetInput: String,
    onOffsetSelected: (Int) -> Unit,
    onCustomOffsetSelected: () -> Unit,
    onCustomOffsetInput: (String) -> Unit,
    onSetAlarm: () -> Unit,
    scheduledAlarmTime: String?
) {
    val offsetOptions = listOf(
        Pair("15 mins", 15),
        Pair("30 mins", 30),
        Pair("45 mins", 45),
        Pair("1 Hour", 60)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Alert Warning Offset",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                offsetOptions.take(3).forEach { option ->
                    val isSelected = !isCustomOffsetSelected && warningOffsetMinutes == option.second
                    FilterChip(
                        selected = isSelected,
                        onClick = { onOffsetSelected(option.second) },
                        label = { Text(option.first) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                offsetOptions.drop(3).forEach { option ->
                    val isSelected = !isCustomOffsetSelected && warningOffsetMinutes == option.second
                    FilterChip(
                        selected = isSelected,
                        onClick = { onOffsetSelected(option.second) },
                        label = { Text(option.first) }
                    )
                }

                FilterChip(
                    selected = isCustomOffsetSelected,
                    onClick = onCustomOffsetSelected,
                    label = { Text("Custom Minutes") }
                )
            }

            if (isCustomOffsetSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customOffsetInput,
                    onValueChange = onCustomOffsetInput,
                    label = { Text("Warning Offset (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSetAlarm,
                modifier = Modifier
                    .fillMaxWidth()
                    // M3 Expressive Medium tier (56dp) for the primary hero action.
                    .height(ButtonDefaults.MediumContainerHeight),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Alarm Reminder")
            }

            scheduledAlarmTime?.let { alarm ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Alarm Active",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Notification scheduled for: $alarm",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}
