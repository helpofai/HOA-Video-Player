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
package com.helpofai.videoplayer.feature.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.core.model.Video

@Composable
fun AdvancedVideoTags(
    video: Video,
    modifier: Modifier = Modifier
) {
    val isNew = video.playCount == 0
    val isViewed = video.playCount > 0
    val is4K = video.size > 800_000_000L
    val isHDR = video.size > 1_500_000_000L

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isNew) {
            BadgeTag(
                text = "NEW",
                gradient = Brush.linearGradient(listOf(Color(0xFFFF0055), Color(0xFFFF5500)))
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (isViewed) {
            BadgeTag(
                text = "VIEWED",
                gradient = Brush.linearGradient(listOf(Color(0xFF555555), Color(0xFF333333))),
                outlineOnly = true
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        if (is4K) {
            BadgeTag(
                text = "4K",
                gradient = Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (video.size > 200_000_000L) {
            BadgeTag(
                text = "HD",
                gradient = Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        if (isHDR) {
            BadgeTag(
                text = "HDR",
                gradient = Brush.linearGradient(listOf(Color(0xFFF2C94C), Color(0xFFF2994A))),
                textColor = Color.Black
            )
        }
    }
}

@Composable
fun BadgeTag(
    text: String,
    gradient: Brush,
    textColor: Color = Color.White,
    outlineOnly: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (outlineOnly) {
                    Modifier.border(1.dp, gradient, RoundedCornerShape(4.dp))
                } else {
                    Modifier.background(gradient)
                }
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (outlineOnly) Color.LightGray else textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}
