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
    var progress by remember { mutableStateOf(0f) }
    
    // Simulate loading progress
    LaunchedEffect(Unit) {
        val totalTime = 800L // 0.8 seconds loading
        val interval = 25L
        val steps = totalTime / interval
        for (i in 0..steps) {
            progress = i.toFloat() / steps.toFloat()
            delay(interval)
        }
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
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(7.dp))
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
