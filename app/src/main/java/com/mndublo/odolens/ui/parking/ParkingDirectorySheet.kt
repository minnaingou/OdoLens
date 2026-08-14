package com.mndublo.odolens.ui.parking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mndublo.odolens.data.ParkingPlace
import java.util.Locale

private fun formatHoursLabel(minutes: Int): String {
    return when {
        minutes == 0 -> "0 h"
        minutes % 60 == 0 -> "${minutes / 60} ${if (minutes / 60 == 1) "h" else "h"}"
        else -> String.format(Locale.getDefault(), "%.1f h", minutes / 60f)
    }
}

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)
/**
 * Bottom sheet that lets the user manage their Parking Place Directory.
 *
 * - Tap a row to select it and dismiss the sheet.
 * - Edit icon opens inline form pre-filled with the entry's values.
 * - Delete icon shows a confirmation dialog.
 * - The "+" button at the top opens a blank inline form to add a new entry.
 */
@Composable
fun ParkingDirectorySheet(
    directory: List<ParkingPlace>,
    selectedEntryId: String?,
    onSelect: (ParkingPlace) -> Unit,
    onAdd: (name: String, freeMinutes: Int) -> Unit,
    onEdit: (id: String, name: String, freeMinutes: Int) -> Unit,
    onDelete: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Inline form state
    var formMode by remember { mutableStateOf<FormMode>(FormMode.Hidden) }
    var formName by remember { mutableStateOf("") }
    var formHours by remember { mutableFloatStateOf(2f) }

    // Delete confirmation
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    val filteredPlaces = remember(directory, searchQuery, isSearchActive) {
        val list = if (!isSearchActive || searchQuery.isBlank()) {
            directory
        } else {
            directory.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
        list.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    fun openAdd() {
        formName = ""
        formHours = 2f
        formMode = FormMode.Add
    }

    fun openEdit(place: ParkingPlace) {
        formName = place.name
        formHours = place.freeMinutes / 60f
        formMode = FormMode.Edit(place.id)
    }

    fun closeForm() {
        keyboard?.hide()
        formMode = FormMode.Hidden
    }

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
                .padding(bottom = 32.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Parking Place Directory",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Tap a place to apply its free hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (directory.isNotEmpty()) {
                        FilledTonalIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) {
                                    searchQuery = ""
                                    keyboard?.hide()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = if (isSearchActive) "Close search" else "Search places"
                            )
                        }
                    }
                    FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); openAdd() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add place")
                    }
                }
            }

            // ── Toggleable Search Box ───────────────────────────────────────
            AnimatedVisibility(
                visible = isSearchActive && formMode == FormMode.Hidden,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search places…") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            } else {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    keyboard?.hide()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            // ── Inline add/edit form ─────────────────────────────────────
            AnimatedVisibility(
                visible = formMode != FormMode.Hidden,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (formMode is FormMode.Add) "New Place" else "Edit Place",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = formName,
                        onValueChange = { formName = it },
                        label = { Text("Place name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val formMinutes = (formHours * 60).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Free hours", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = formatHoursLabel(formMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    var lastStep by remember { mutableIntStateOf(-1) }
                    Slider(
                        value = formHours,
                        onValueChange = { h ->
                            val step = (h * 2).toInt()
                            if (step != lastStep) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastStep = step
                            }
                            formHours = h
                        },
                        valueRange = 0f..8f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = { closeForm() }) { Text("Cancel") }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val mins = (formHours * 60).toInt()
                                when (val m = formMode) {
                                    is FormMode.Add -> onAdd(formName.trim(), mins)
                                    is FormMode.Edit -> onEdit(m.id, formName.trim(), mins)
                                    FormMode.Hidden -> {}
                                }
                                closeForm()
                            },
                            enabled = formName.isNotBlank(),
                            modifier = Modifier.height(ButtonDefaults.MediumContainerHeight)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Save")
                        }
                    }
                    HorizontalDivider()
                }
            }

            // ── Directory list ───────────────────────────────────────────
            if (directory.isEmpty() && formMode == FormMode.Hidden) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No places saved yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tap  +  to add a place with its free hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (filteredPlaces.isEmpty() && formMode == FormMode.Hidden) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No places found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(filteredPlaces, key = { it.id }) { place ->
                        val isSelected = place.id == selectedEntryId

                        @Suppress("DEPRECATION")
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    confirmDeleteId = place.id
                                    // Return false — we show a confirmation dialog instead of
                                    // instantly removing, so we snap back to Settled.
                                    false
                                } else false
                            },
                            positionalThreshold = { total -> total * 0.5f }
                        )

                        // Haptic on threshold crossing (fires when targetValue leaves Settled)
                        LaunchedEffect(dismissState.targetValue) {
                            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val isThresholdReached =
                                    dismissState.targetValue != SwipeToDismissBoxValue.Settled
                                val bgColor by animateColorAsState(
                                    if (isThresholdReached) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    label = "swipeBg"
                                )
                                val iconScale by animateFloatAsState(
                                    if (isThresholdReached) 1.3f else 1.0f,
                                    label = "swipeScale"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(bgColor)
                                        .padding(end = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = if (isThresholdReached)
                                                MaterialTheme.colorScheme.onError
                                            else
                                                MaterialTheme.colorScheme.error,
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
                            },
                            content = {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = place.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = "${formatHoursLabel(place.freeMinutes)} free",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    leadingContent = if (isSelected) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else null,
                                    trailingContent = {
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            openEdit(place)
                                        }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSelect(place)
                                        onDismiss()
                                    }
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ───────────────────────────────────────
    confirmDeleteId?.let { idToDelete ->
        val placeName = directory.find { it.id == idToDelete }?.name ?: ""
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Remove place?") },
            text = {
                Text("\"$placeName\" will be removed from your directory.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete(idToDelete)
                        confirmDeleteId = null
                    }
                ) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}

/** Discriminated union for the inline form mode inside [ParkingDirectorySheet]. */
private sealed interface FormMode {
    data object Hidden : FormMode
    data object Add : FormMode
    data class Edit(val id: String) : FormMode
}
