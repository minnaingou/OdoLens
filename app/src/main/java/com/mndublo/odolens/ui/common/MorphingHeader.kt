package com.mndublo.odolens.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Morphing Camera Button:
 * Physically glides and squeezes from a wide labeled pill into a compact round icon button.
 */
@Composable
private fun MorphingCameraButton(
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "camPressScale"
    )

    val textAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(
            horizontal = lerp(16.dp, 0.dp, progress),
            vertical = 0.dp
        ),
        shape = RoundedCornerShape(lerp(24.dp, 20.dp, progress)),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier.scale(pressScale)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Camera",
                modifier = Modifier.size(20.dp)
            )
            if (textAlpha > 0.05f) {
                Spacer(modifier = Modifier.width(lerp(8.dp, 0.dp, progress)))
                Text(
                    text = stringResource(com.mndublo.odolens.R.string.source_camera),
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer { alpha = textAlpha }
                )
            }
        }
    }
}

/**
 * Morphing Gallery Button:
 * Physically glides and squeezes from a wide labeled pill into a compact tonal icon button.
 */
@Composable
private fun MorphingGalleryButton(
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "galleryPressScale"
    )

    val textAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)

    FilledTonalButton(
        onClick = onClick,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(
            horizontal = lerp(16.dp, 0.dp, progress),
            vertical = 0.dp
        ),
        shape = RoundedCornerShape(lerp(24.dp, 20.dp, progress)),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier.scale(pressScale)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Gallery",
                modifier = Modifier.size(20.dp)
            )
            if (textAlpha > 0.05f) {
                Spacer(modifier = Modifier.width(lerp(8.dp, 0.dp, progress)))
                Text(
                    text = stringResource(com.mndublo.odolens.R.string.source_gallery),
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer { alpha = textAlpha }
                )
            }
        }
    }
}

/**
 * True Continuous Physical Geometry Morphing Header for Trips (Dashboard).
 * Standout primaryContainer accent surface with signature onPrimaryContainer styling.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingDashboardHeader(
    progress: Float,
    fuelPrice: Double,
    fuelPriceDate: String,
    onEditPrice: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    isLoading: Boolean,
    statusMessage: String?,
    modifier: Modifier = Modifier
) {
    val totalHeight = lerp(184.dp, 56.dp, progress)
    val cornerRadius = lerp(28.dp, 24.dp, progress)
    val subtitleAlpha = (1f - progress * 3f).coerceIn(0f, 1f)
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = lerp(2.dp, 6.dp, progress),
        shadowElevation = lerp(0.dp, 4.dp, progress),
        shape = RoundedCornerShape(cornerRadius),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = lerp(6.dp, 4.dp, progress))
            .height(totalHeight)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = lerp(16.dp, 12.dp, progress),
                    vertical = lerp(14.dp, 8.dp, progress)
                )
        ) {
            val contentWidth = maxWidth

            // 1. Gas Station Glyph
            val iconBoxSize = lerp(40.dp, 22.dp, progress)
            val iconGlyphSize = lerp(22.dp, 16.dp, progress)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = 0.dp, y = lerp(0.dp, 6.dp, progress))
                    .size(iconBoxSize)
                    .background(
                        color = onPrimaryContainer.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = onPrimaryContainer,
                    modifier = Modifier.size(iconGlyphSize)
                )
            }

            // 2. Subtitle: "Fuel Price" + Date Pill (Left-aligned, fades out quickly on scroll)
            if (subtitleAlpha > 0.05f) {
                Column(
                    modifier = Modifier
                        .offset(x = 48.dp, y = 0.dp)
                        .graphicsLayer { alpha = subtitleAlpha }
                ) {
                    Text(
                        text = "Fuel Price",
                        style = MaterialTheme.typography.titleSmall,
                        color = onPrimaryContainer
                    )
                    if (fuelPriceDate.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    onPrimaryContainer.copy(alpha = 0.12f),
                                    MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = fuelPriceDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // 3. Hero Price Numeral ("38.50 THB/L"): Prominent on the right when expanded, glides to the left when collapsed
            val priceExpandedX = (contentWidth - 184.dp).coerceAtLeast(110.dp)
            val priceX = lerp(priceExpandedX, 28.dp, progress)
            val priceY = lerp(0.dp, 6.dp, progress)
            val priceFontSize = lerp(28.sp, 16.sp, progress)

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .offset(x = priceX, y = priceY)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onEditPrice)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%.2f", fuelPrice),
                    fontSize = priceFontSize,
                    fontWeight = FontWeight.Bold,
                    color = onPrimaryContainer,
                    lineHeight = priceFontSize
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "THB/L",
                    style = if (progress > 0.5f) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall,
                    color = onPrimaryContainer,
                    modifier = Modifier.padding(bottom = lerp(4.dp, 0.dp, progress))
                )
                if (progress > 0.4f) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Fuel Price",
                        tint = onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // 4. Circular Edit Button (Only visible in expanded state)
            if (subtitleAlpha > 0.05f) {
                IconButton(
                    onClick = onEditPrice,
                    modifier = Modifier
                        .offset(x = contentWidth - 38.dp, y = 0.dp)
                        .size(38.dp)
                        .graphicsLayer { alpha = subtitleAlpha }
                        .background(onPrimaryContainer.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Fuel Price",
                        tint = onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 5. Zone 2: "Log New Trip" Action Header with Subtitle
            val titleFontSize = lerp(18.sp, 16.sp, progress)
            if (subtitleAlpha > 0.05f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .offset(x = 0.dp, y = 56.dp)
                        .graphicsLayer { alpha = subtitleAlpha }
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Log New Trip",
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = onPrimaryContainer,
                            lineHeight = titleFontSize
                        )
                        Text(
                            text = "Scan dashboard trip or odometer",
                            style = MaterialTheme.typography.bodySmall,
                            color = onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // 6. Camera Button: Glides diagonally and shrinks in width & height
            val camStartX = 0.dp
            val camStartY = 106.dp
            val camStartW = (contentWidth - 8.dp) / 2
            val camStartH = 48.dp

            val camEndX = contentWidth - 88.dp
            val camEndY = 0.dp
            val camEndW = 40.dp
            val camEndH = 40.dp

            val camX = lerp(camStartX, camEndX, progress)
            val camY = lerp(camStartY, camEndY, progress)
            val camW = lerp(camStartW, camEndW, progress)
            val camH = lerp(camStartH, camEndH, progress)

            MorphingCameraButton(
                progress = progress,
                onClick = onCamera,
                modifier = Modifier
                    .offset(x = camX, y = camY)
                    .width(camW)
                    .height(camH)
            )

            // 7. Gallery Button: Glides diagonally and shrinks in width & height
            val galStartX = (contentWidth + 8.dp) / 2
            val galStartY = 106.dp
            val galStartW = (contentWidth - 8.dp) / 2
            val galStartH = 48.dp

            val galEndX = contentWidth - 40.dp
            val galEndY = 0.dp
            val galEndW = 40.dp
            val galEndH = 40.dp

            val galX = lerp(galStartX, galEndX, progress)
            val galY = lerp(galStartY, galEndY, progress)
            val galW = lerp(galStartW, galEndW, progress)
            val galH = lerp(galStartH, galEndH, progress)

            MorphingGalleryButton(
                progress = progress,
                onClick = onGallery,
                modifier = Modifier
                    .offset(x = galX, y = galY)
                    .width(galW)
                    .height(galH)
            )

            // 8. Progress Indicator (Only visible when loading)
            if (isLoading && subtitleAlpha > 0.05f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .offset(x = 0.dp, y = 158.dp)
                        .graphicsLayer { alpha = subtitleAlpha }
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMessage ?: "Processing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * True Continuous Physical Geometry Morphing Header for Parking.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingParkingHeader(
    progress: Float,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    isAiLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val totalHeight = lerp(148.dp, 56.dp, progress)
    val cornerRadius = lerp(28.dp, 24.dp, progress)
    val subtitleAlpha = (1f - progress * 3f).coerceIn(0f, 1f)
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = lerp(2.dp, 6.dp, progress),
        shadowElevation = lerp(0.dp, 4.dp, progress),
        shape = RoundedCornerShape(cornerRadius),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = lerp(6.dp, 4.dp, progress))
            .height(totalHeight)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = lerp(16.dp, 12.dp, progress),
                    vertical = lerp(14.dp, 8.dp, progress)
                )
        ) {
            val contentWidth = maxWidth

            // 1. Ticket Icon
            val iconBoxSize = lerp(40.dp, 24.dp, progress)
            val iconGlyphSize = lerp(22.dp, 18.dp, progress)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = 0.dp, y = lerp(0.dp, 8.dp, progress))
                    .size(iconBoxSize)
                    .background(
                        color = onPrimaryContainer.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ConfirmationNumber,
                    contentDescription = null,
                    tint = onPrimaryContainer,
                    modifier = Modifier.size(iconGlyphSize)
                )
            }

            // 2. Title & Subtitle (Translates and morphs into compact title)
            val titleX = lerp(48.dp, 30.dp, progress)
            val titleY = lerp(0.dp, 8.dp, progress)
            val titleFontSize = lerp(18.sp, 16.sp, progress)

            Column(
                modifier = Modifier.offset(x = titleX, y = titleY)
            ) {
                Text(
                    text = if (progress > 0.5f) "Scan Ticket" else "Scan Parking Ticket",
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = onPrimaryContainer,
                    lineHeight = titleFontSize
                )
                if (subtitleAlpha > 0.05f) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Read your ticket to start the timer",
                        style = MaterialTheme.typography.bodySmall,
                        color = onPrimaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                    )
                }
            }

            // 3. Camera Button: Glides diagonally and shrinks in width & height
            val camStartX = 0.dp
            val camStartY = 70.dp
            val camStartW = (contentWidth - 8.dp) / 2
            val camStartH = 48.dp

            val camEndX = contentWidth - 88.dp
            val camEndY = 0.dp
            val camEndW = 40.dp
            val camEndH = 40.dp

            val camX = lerp(camStartX, camEndX, progress)
            val camY = lerp(camStartY, camEndY, progress)
            val camW = lerp(camStartW, camEndW, progress)
            val camH = lerp(camStartH, camEndH, progress)

            MorphingCameraButton(
                progress = progress,
                onClick = onCamera,
                modifier = Modifier
                    .offset(x = camX, y = camY)
                    .width(camW)
                    .height(camH)
            )

            // 4. Gallery Button: Glides diagonally and shrinks in width & height
            val galStartX = (contentWidth + 8.dp) / 2
            val galStartY = 70.dp
            val galStartW = (contentWidth - 8.dp) / 2
            val galStartH = 48.dp

            val galEndX = contentWidth - 40.dp
            val galEndY = 0.dp
            val galEndW = 40.dp
            val galEndH = 40.dp

            val galX = lerp(galStartX, galEndX, progress)
            val galY = lerp(galStartY, galEndY, progress)
            val galW = lerp(galStartW, galEndW, progress)
            val galH = lerp(galStartH, galEndH, progress)

            MorphingGalleryButton(
                progress = progress,
                onClick = onGallery,
                modifier = Modifier
                    .offset(x = galX, y = galY)
                    .width(galW)
                    .height(galH)
            )

            // 5. Loading State
            if (isAiLoading && subtitleAlpha > 0.05f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .offset(x = 0.dp, y = 124.dp)
                        .graphicsLayer { alpha = subtitleAlpha }
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Analyzing Ticket with Gemini AI...",
                        style = MaterialTheme.typography.bodySmall,
                        color = onPrimaryContainer
                    )
                }
            }
        }
    }
}
