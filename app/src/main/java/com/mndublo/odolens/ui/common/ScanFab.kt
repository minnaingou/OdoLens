package com.mndublo.odolens.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

/**
 * Shared expressive scan FAB (Trips + Parking): 56dp Extended FAB in `primaryContainer`
 * with a springy press-scale — the M3 Expressive press motion, matching the app's
 * spring-based motion language.
 */
@Composable
fun ScanFab(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scanFabScale"
    )

    ExtendedFloatingActionButton(
        onClick = onClick,
        interactionSource = interactionSource,
        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera") },
        text = { Text(text) },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        ),
        modifier = Modifier.scale(scale)
    )
}
