/*
|--------------------------------------------------------------------------
| HelpOfAi (HOA) Professional Software
|--------------------------------------------------------------------------
|
| Copyright (c) 2026 Rajib Adhikary. All Rights Reserved.
|
| This file is part of the HelpOfAi Professional Software Suite.
| Unauthorized copying, modification, redistribution, reverse engineering,
| decompilation, or commercial use of this source code, in whole or in part,
| is strictly prohibited without prior written permission from the copyright owner.
|
| Author      : Rajib Adhikary
| Organization: HelpOfAi (HOA)
| Website     : https://helpofai.com
| Location    : Basta Purba Para, Aranghata, Nadia, West Bengal, India
|
| This source code contains proprietary and confidential information.
| Any unauthorized access or distribution may violate applicable copyright laws.
|
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.feature.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Advanced Professional Decoder Badge (HW / HW+ / SW)
 *
 * Displays active hardware/software decoding engine in a high-tech glassmorphism pill.
 * - Single Tap: Opens full DecoderSelectorSheet for detailed codec options.
 * - Long Press: Instantly cycles between HW -> HW+ -> SW modes with tactile response.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DecoderBadge(
    decoderMode: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val mode = decoderMode.uppercase()

    val (accentColor, glowColor, bgGradient, icon: ImageVector) = when (mode) {
        "SW" -> Quad(
            Color(0xFFFFB74D), // Cyber Amber
            Color(0xFFFFB74D).copy(alpha = 0.35f),
            listOf(Color(0xE62A1900), Color(0xCC1A1000)),
            Icons.Default.Code
        )
        "HW+" -> Quad(
            Color(0xFFB388FF), // Neon Lavender / Violet
            Color(0xFFB388FF).copy(alpha = 0.35f),
            listOf(Color(0xE61F0D36), Color(0xCC120722)),
            Icons.Default.DeveloperBoard
        )
        else -> Quad(
            Color(0xFF00E5FF), // Electric Neon Cyan (Default HW)
            Color(0xFF00E5FF).copy(alpha = 0.35f),
            listOf(Color(0xE6002030), Color(0xCC001420)),
            Icons.Default.Memory
        )
    }

    // Subtle pulsing animation for live decoder engine indicator
    val infiniteTransition = rememberInfiniteTransition(label = "DecoderPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(bgGradient))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Live status glowing dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = pulseAlpha))
                )

                // Decoder Type Icon
                Icon(
                    imageVector = icon,
                    contentDescription = "$mode Decoder",
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )

                // High-Tech Monospace Label
                Text(
                    text = mode,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.6.sp
                )

                // Mini Dropdown Indicator
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Decoder",
                    tint = accentColor.copy(alpha = 0.75f),
                    modifier = Modifier
                        .size(14.dp)
                        .offset(x = (-2).dp)
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
