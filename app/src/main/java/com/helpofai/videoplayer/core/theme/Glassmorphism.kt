package com.helpofai.videoplayer.core.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An advanced modifier that creates a premium Glassmorphism (Frosted Glass) effect.
 * Features a semi-transparent surface and an ultra-thin reflective border.
 */
fun Modifier.frostedGlass(
    cornerRadius: Dp = 24.dp,
    surfaceAlpha: Float = 0.2f,
    borderAlpha: Float = 0.15f,
    surfaceColor: Color = Color.White
): Modifier = composed {
    this.then(
        Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(surfaceColor.copy(alpha = surfaceAlpha))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = borderAlpha)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    )
}
