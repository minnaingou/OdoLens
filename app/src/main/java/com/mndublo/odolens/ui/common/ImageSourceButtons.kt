package com.mndublo.odolens.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Row of "Camera" / "Gallery" buttons used to pick the image source for a scan.
 * Shared by the Dashboard (trip scan) and Parking (ticket scan) screens.
 * Camera carries a springy press-scale (M3 Expressive motion) to signal primary action.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImageSourceButtons(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cameraScale"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onCamera,
            interactionSource = interactionSource,
            // M3 Expressive Medium tier (56dp); hierarchy via filled-primary color, not size.
            modifier = Modifier
                .weight(1f)
                .height(ButtonDefaults.MediumContainerHeight)
                .scale(scale)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(com.mndublo.odolens.R.string.source_camera))
        }
        FilledTonalButton(
            onClick = onGallery,
            modifier = Modifier
                .weight(1f)
                .height(ButtonDefaults.MediumContainerHeight)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(com.mndublo.odolens.R.string.source_gallery))
        }
    }
}
