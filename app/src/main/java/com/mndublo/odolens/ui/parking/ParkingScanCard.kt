package com.mndublo.odolens.ui.parking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mndublo.odolens.ui.common.ImageSourceButtons

/** "Scan Parking Ticket" card: camera/gallery picker and AI-parse progress. */
@Composable
fun ParkingScanCard(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    isAiLoading: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Scan Parking Ticket",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ImageSourceButtons(
                onCamera = onCamera,
                onGallery = onGallery
            )

            AnimatedVisibility(
                visible = isAiLoading,
                enter = fadeIn(animationSpec = tween(durationMillis = 250)) +
                    expandVertically(animationSpec = tween(durationMillis = 250))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analyzing Ticket with Gemini AI...")
                    }
                }
            }
        }
    }
}
