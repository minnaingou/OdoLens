package com.mndublo.odolens.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

/** "Trip Details" form: distance/economy/name/price inputs, cost preview and save action. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TripDetailsForm(
    distanceInput: String,
    onDistanceChange: (String) -> Unit,
    economyInput: String,
    onEconomyChange: (String) -> Unit,
    tripNameInput: String,
    onTripNameChange: (String) -> Unit,
    fuelPriceInput: String,
    onFuelPriceChange: (String) -> Unit,
    calculatedCost: Double,
    onSave: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Trip Details",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = distanceInput,
                onValueChange = onDistanceChange,
                label = { Text("Distance (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = economyInput,
                onValueChange = onEconomyChange,
                label = { Text("Fuel Economy (km/L)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocalGasStation, contentDescription = null) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = tripNameInput,
                onValueChange = onTripNameChange,
                label = { Text("Trip Name (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = fuelPriceInput,
                onValueChange = onFuelPriceChange,
                label = { Text("Fuel Price (THB/L)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calculated Cost Panel
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
                    text = "Estimated Cost:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.2f THB", calculatedCost),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave()
                },
                // M3 Expressive Medium tier (56dp) for the primary hero action.
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonDefaults.MediumContainerHeight),
                enabled = distanceInput.isNotBlank() && economyInput.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Trip")
            }
        }
    }
}
