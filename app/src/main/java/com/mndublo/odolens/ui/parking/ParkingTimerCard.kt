package com.mndublo.odolens.ui.parking

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber

/**
 * Active-timer card / Expired-timer card:
 * - When active: live countdown ring rendered with the native M3 expressive
 *   [CircularWavyProgressIndicator], plus expiry / alert / spot summary and extend + reset actions.
 * - When expired: high-visibility warning card with "PARKING EXPIRED", live "Expired X ago" indicator,
 *   parking details, and an acknowledgment button to clear and return to the entry form.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ParkingTimerCard(
    countdownText: String,
    calculatedExpiry: String,
    scheduledAlarmTime: String?,
    parkingSpotNote: String,
    progressFraction: Float = 1f,
    isExpired: Boolean = false,
    expiredAgoText: String = "",
    onExtend: () -> Unit,
    onReset: () -> Unit
) {
    val containerColor = if (isExpired) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isExpired) {
                // Header: Warning / Expired
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PARKING EXPIRED",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero Elapsed Time Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (expiredAgoText.isNotBlank()) "Expired $expiredAgoText ago" else "Expired",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Free parking period has ended",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Expired At", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Text(calculatedExpiry, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }

                    if (parkingSpotNote.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Parking Spot", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            Text(parkingSpotNote, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ButtonDefaults.MediumContainerHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear & Done", style = MaterialTheme.typography.titleSmall)
                }
            } else {
                // Header: Active Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PARKING TIMER ACTIVE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                CountdownRing(
                    countdownText = countdownText,
                    progressFraction = progressFraction
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Expires At", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(calculatedExpiry, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }

                    scheduledAlarmTime?.let { alarm ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Alert Set For", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(alarm, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (parkingSpotNote.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Parking Spot", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(parkingSpotNote, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onExtend,
                    modifier = Modifier
                        .fillMaxWidth()
                        // M3 Expressive Medium tier (56dp) for the primary hero action.
                        .height(ButtonDefaults.MediumContainerHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.AddAlarm, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extend Free Parking")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onReset,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear & Reset Parking")
                }
            }
        }
    }
}

/**
 * The countdown hero: a native M3 expressive wavy circular indicator that depletes with the
 * remaining time, with the "HH:MM:SS" text centered inside it. Progress moves with the M3
 * expressive spring ([ProgressIndicatorDefaults.ProgressAnimationSpec]); the wave color shifts to
 * the error role under 25% remaining.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CountdownRing(
    countdownText: String,
    progressFraction: Float
) {
    val progress = progressFraction.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "countdownProgress"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (progress <= 0.25f) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 600),
        label = "countdownColor"
    )
    // Wavy strokes take pixel widths; convert the previous 16.dp ring thickness once per
    // composition and reuse it for both the wave and its track so they stay aligned.
    val density = LocalDensity.current
    val ringStroke = remember(density) {
        Stroke(width = with(density) { 16.dp.toPx() }, cap = StrokeCap.Round)
    }

    // The wavy shape is a circle<->ripple Morph that is only ever *created* the first time the
    // amplitude target changes (see DeterminateCircularWavyProgressNode) — a constant amplitude,
    // even a non-zero one, renders as a plain flat circle forever. Relying on `progress == 1f`
    // for that first "0 -> non-zero" change is fragile: if this composable first enters
    // composition after the countdown has already ticked below 100% (very common — state is
    // computed a frame before Compose picks it up, or the screen is reopened mid-timer), the
    // very first frame already has progress < 1f and amplitude is a constant 0.5f from frame
    // one, so the Morph never gets created and the wave never appears.
    //
    // Fix: decouple the transition from `progress` entirely. `waveArmed` starts false (so the
    // first composed frame always renders amplitude 0f, i.e. a flat circle) and flips true one
    // frame later via LaunchedEffect, guaranteeing a real 0 -> 0.5 change happens exactly once
    // regardless of what progress the timer starts at.
    var waveArmed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        waveArmed = true
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(280.dp)
            .padding(4.dp)
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = indicatorColor,
            // Neutral, low-alpha track: keeps the full circle visible while reading as an
            // intentional track on the solid surfaceVariant card (not a tinted background).
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
            stroke = ringStroke,
            trackStroke = ringStroke,
            gapSize = 8.dp,
            // Wave geometry follows the official M3 Expressive demo recipe
            // (CircularWavyProgressIndicatorSample): amplitude 1.0 = the full default wave depth,
            // and ~9 undulations around the ring. The token defaults are tuned for the 48dp
            // reference container — 15dp wavelength there yields 2*pi*(24-2)/15 ~= 9 waves, so
            // here (272dp ring, 16dp stroke -> centerline radius 128dp) the wavelength is scaled
            // to keep the same ~9 clean waves: 2*pi*128/9 ~= 88dp. The smaller 15dp default would
            // pack ~53 waves onto this ring and read as a knurled edge.
            wavelength = 88.dp,
            // A constant amplitude would render NO wave: the wavy shape is a circle<->ripple
            // Morph that only gets created when the amplitude target *changes* (see
            // DeterminateCircularWavyProgressNode). So: flat only until waveArmed flips, then a
            // one-time 0 -> 1 ripple-in, held for the whole countdown. 1f is the demo default
            // (wave depth = 25% of the ring radius); scale down (e.g. 0.6f) for a subtler wave.
            amplitude = { if (waveArmed && progress < 1f) 1f else 0f }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = countdownText.ifEmpty { "--:--" },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Time Remaining",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}