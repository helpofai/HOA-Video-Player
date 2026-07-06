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
    // 1. Status Tags (New, Continue, Viewed)
    val isNew = video.playCount == 0
    val isViewed = video.playCount > 0 && video.lastPlayedPosition >= video.duration * 0.95
    val isContinue = video.playCount > 0 && video.lastPlayedPosition > 0 && video.lastPlayedPosition < video.duration * 0.95

    // 2. Resolution Tags (8K, 4K, 1080p, 720p, SD)
    val maxRes = maxOf(video.width, video.height)
    val is8K = maxRes >= 7680
    val is4K = maxRes >= 3840 && maxRes < 7680
    val is1080p = maxRes >= 1920 && maxRes < 3840
    val is720p = maxRes >= 1280 && maxRes < 1920
    val isSD = maxRes > 0 && maxRes < 1280

    // 3. Format Tags (HDR, UHD, FHD)
    val isHDR = video.title.contains("hdr", ignoreCase = true) || video.size > 2_000_000_000L

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Status Badge ---
        if (isNew) {
            BadgeTag(
                text = "NEW",
                gradient = Brush.linearGradient(listOf(Color(0xFFFF0055), Color(0xFFFF5500)))
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (isContinue) {
            BadgeTag(
                text = "CONTINUE",
                gradient = Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))
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

        // --- Resolution Badge ---
        if (is8K) {
            BadgeTag(
                text = "8K UHD",
                gradient = Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)))
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (is4K) {
            BadgeTag(
                text = "4K UHD",
                gradient = Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D)))
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (is1080p) {
            BadgeTag(
                text = "1080p FHD",
                gradient = Brush.linearGradient(listOf(Color(0xFF4CA1AF), Color(0xFFC4E0E5))),
                textColor = Color(0xFF020617)
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (is720p) {
            BadgeTag(
                text = "720p HD",
                gradient = Brush.linearGradient(listOf(Color(0xFF3A7BD5), Color(0xFF3A6073)))
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (isSD) {
            BadgeTag(
                text = "SD",
                gradient = Brush.linearGradient(listOf(Color(0xFF4B79A1), Color(0xFF283E51)))
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // --- HDR Badge ---
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
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (outlineOnly) {
                    Modifier.border(1.dp, gradient, RoundedCornerShape(6.dp))
                } else {
                    Modifier.background(gradient)
                }
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (outlineOnly) Color.LightGray else textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
    }
}
