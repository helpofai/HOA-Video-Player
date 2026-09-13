package com.helpofai.videoplayer.core.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-performance Glassmorphism modifier without composed {} overhead.
 * Uses a pure modifier chain to eliminate Compose node inspection and lambda churn.
 */
fun Modifier.frostedGlass(
    cornerRadius: Dp = 24.dp,
    surfaceAlpha: Float = 0.2f,
    borderAlpha: Float = 0.15f,
    surfaceColor: Color = Color.White
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = borderAlpha),
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = borderAlpha)
        )
    )
    return this
        .clip(shape)
        .background(surfaceColor.copy(alpha = surfaceAlpha))
        .border(width = 1.dp, brush = borderBrush, shape = shape)
}
