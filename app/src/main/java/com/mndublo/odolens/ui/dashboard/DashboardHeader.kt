package com.mndublo.odolens.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import android.widget.Toast
import java.util.Locale

/** Fuel-price header card with the edit affordance. */
@Composable
fun DashboardHeader(
    fuelPrice: Double,
    fuelPriceDate: String,
    onEditClick: () -> Unit
) {
    Card(
        // Flat tonal surface per the M3 Expressive color system (no gradient tokens).
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Identity chip: tonal circle hosting the gas-station glyph.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(onPrimaryContainer.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fuel Price",
                    style = MaterialTheme.typography.titleSmall,
                    color = onPrimaryContainer
                )
                if (fuelPriceDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    // Small tonal pill carrying the "as of" date.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(onPrimaryContainer.copy(alpha = 0.12f), MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "as of $fuelPriceDate",
                            style = MaterialTheme.typography.labelMedium,
                            color = onPrimaryContainer
                        )
                    }
                }
                // Hero numeral animates when the price changes (spring fade + scale-in).
                AnimatedContent(
                    targetState = String.format(Locale.getDefault(), "%.2f", fuelPrice),
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(durationMillis = 120)) +
                                    scaleOut(targetScale = 0.95f, animationSpec = tween(durationMillis = 120))
                            )
                    },
                    label = "fuelPrice"
                ) { price ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = price,
                            style = MaterialTheme.typography.displayMedium,
                            color = onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "THB/L",
                            style = MaterialTheme.typography.titleMedium,
                            color = onPrimaryContainer,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }
            // Tonal circular edit chip (same recipe as the ErrorCard dismiss chip).
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(onPrimaryContainer.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Fuel Price",
                    tint = onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/** Dialog to edit the active fuel price; only calls [onSave] with a valid positive price. */
@Composable
fun EditFuelPriceDialog(
    initialPrice: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf(initialPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Fuel Price") },
        text = {
            Column {
                Text("Enter new fuel price (THB/L):", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newPrice = input.toDoubleOrNull()
                    if (newPrice != null && newPrice > 0.0) {
                        onSave(newPrice)
                    } else {
                        Toast.makeText(context, "Please enter a valid price.", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
