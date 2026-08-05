package com.mndublo.odolens.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 Expressive shape system — big, friendly rounded corners.
// These are the M3 Expressive corner tokens (dp):
//   extraSmall 8 · small 12 · medium 16 · large 24 · extraLarge 28
// Cards, sheets, FABs and chips inherit these through MaterialTheme.
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
