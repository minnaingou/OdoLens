package com.mndublo.odolens.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mndublo.odolens.R

/**
 * Self-dismissing scan-progress card in the app's M3 Expressive language — the loading
 * counterpart to [ErrorCard]: extraLarge hero radius, tonal primary container, a circular
 * tonal chip hosting the spinner, and a close button to cancel the in-flight scan via
 * [onCancel]. The card is only rendered while a scan is running, so it dismisses itself
 * as soon as the scan finishes (success or failure). Shared by Dashboard and Parking.
 */
@Composable
fun ScanProgressCard(
    message: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Mirrors ErrorCard: surfaces move in rather than popping in, so start hidden and
    // animate to visible one frame later.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 250)) +
            expandVertically(animationSpec = tween(durationMillis = 250))
    ) {
        val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                // Tonal circular chip hosting the progress spinner.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .background(onContainer.copy(alpha = 0.12f), CircleShape)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = onContainer,
                        strokeWidth = 3.dp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = message,
                    color = onContainer,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                // Tonal circular cancel chip.
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(48.dp)
                        .background(onContainer.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.scan_cancel),
                        tint = onContainer
                    )
                }
            }
        }
    }
}
