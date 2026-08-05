package com.mndublo.odolens.ui.parking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mndublo.odolens.data.ParkingTimerPlanner
import java.util.Calendar

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
/** Bottom sheet to extend the running parking timer (12-hour cap enforced by the planner). */
@Composable
fun ExtendParkingSheet(
    freeDurationMinutes: Int,
    onDismiss: () -> Unit,
    onExtend: (hours: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val remainingMinutes = ParkingTimerPlanner.MAX_FREE_MINUTES - freeDurationMinutes
    val maxExtendHours = (remainingMinutes / 60).coerceAtLeast(0)
    var extendHours by remember { mutableIntStateOf(1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Extend Free Parking",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (maxExtendHours <= 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Maximum 12-hour limit reached.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Close") }
            } else {
                val usedHours = freeDurationMinutes / 60
                Text(
                    text = "${usedHours}h used · ${maxExtendHours}h remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "+${extendHours}h",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                val presets = buildList {
                    if (maxExtendHours >= 1) add(Pair("+1 Hour", 1))
                    if (maxExtendHours >= 2) add(Pair("+2 Hours", 2))
                    if (maxExtendHours >= 3) add(Pair("+3 Hours", 3))
                }
                if (presets.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        presets.forEach { (label, hrs) ->
                            FilterChip(
                                selected = extendHours == hrs,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    extendHours = hrs
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val sliderSteps = (maxExtendHours - 1).coerceAtLeast(0)
                Slider(
                    value = extendHours.toFloat().coerceIn(1f, maxExtendHours.toFloat()),
                    onValueChange = { v ->
                        val newHours = v.toInt().coerceIn(1, maxExtendHours)
                        if (newHours != extendHours) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            extendHours = newHours
                        }
                    },
                    valueRange = 1f..maxExtendHours.toFloat(),
                    steps = sliderSteps,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${maxExtendHours}h max", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                        onExtend(extendHours)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.AddAlarm, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Extend by ${extendHours}h",
                    )
                }
            }
        }
    }
}

/** Time-picker dialog for the parking start time. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ParkingTimePickerDialog(
    startTimeInput: String,
    use12h: Boolean,
    onTimePicked: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val initialParts = startTimeInput.split(":")
    val initialHour = initialParts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val initialMinute = initialParts.getOrNull(1)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.MINUTE)
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = !use12h
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Parking Start Time") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTimePicked(timePickerState.hour, timePickerState.minute)
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
