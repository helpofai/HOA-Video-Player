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
package com.helpofai.videoplayer.feature.splash

import android.webkit.WebView
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.graphics.res.animatedVectorResource
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashScreen(onSplashFinished: () -> Unit) {
    val progressAnim = remember { Animatable(0f) }
    val progress = progressAnim.value
    
    // Simulate loading progress using idiomatic Compose animation
    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = LinearEasing)
        )
        delay(100) // Brief pause at 100%
        onSplashFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617)), // Matches the deep obsidian edge of the SVG
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. The Native Animated Vector Logo
        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(RoundedCornerShape(40.dp))
        ) {
            @OptIn(androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi::class)
            val image = androidx.compose.animation.graphics.vector.AnimatedImageVector.animatedVectorResource(id = com.helpofai.videoplayer.R.drawable.ic_logo_animated)
            var atEnd by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                atEnd = true
            }

            @OptIn(androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi::class)
            androidx.compose.foundation.Image(
                painter = androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(image, atEnd = atEnd),
                contentDescription = "Logo",
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 2. Animated Multi-color App Name
        val infiniteTransition = rememberInfiniteTransition(label = "ColorTransition")
        val color1 by infiniteTransition.animateColor(
            initialValue = Color(0xFF00f2fe),
            targetValue = Color(0xFFf093fb),
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Color1"
        )
        val color2 by infiniteTransition.animateColor(
            initialValue = Color(0xFFf093fb),
            targetValue = Color(0xFF00f2fe),
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Color2"
        )
        
        Text(
            text = "HOA Video Player",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(color1, color2)
                )
            )
        )
        
        Spacer(modifier = Modifier.height(80.dp))
        
        // 3. Multi-color Loading Bar with Percentage
        Text(
            text = "${(progress * 100).toInt()}%",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(7.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00f2fe),
                                Color(0xFF818cf8),
                                Color(0xFFc084fc),
                                Color(0xFFf093fb)
                            )
                        )
                    )
            )
        }
    }
}
