package com.helpofai.videoplayer.feature.player.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
private fun getAnimatedRainbowColor(): Color {
    val transition = rememberInfiniteTransition(label = "rainbow")
    val hue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "hue"
    )
    return Color.hsv(hue = hue, saturation = 0.8f, value = 1f)
}

@Composable
fun PlayerFeedbackOverlay(
    feedback: FeedbackEvent?,
    isLongPressing: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (feedback == null) return

    // Auto-dismiss logic
    var isVisible by remember(feedback.id) { mutableStateOf(true) }
    
    LaunchedEffect(feedback.id, isLongPressing) {
        isVisible = true
        if (!isLongPressing) {
            delay(if (feedback.type == FeedbackType.PLAY_PAUSE) 400 else 800)
            isVisible = false
        }
    }

    // Animation
    val transition = updateTransition(targetState = isVisible, label = "feedback_transition")
    
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 200) },
        label = "alpha"
    ) { visible -> if (visible) 1f else 0f }
    
    val scale by transition.animateFloat(
        transitionSpec = { 
            if (targetState) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            else tween(200)
        },
        label = "scale"
    ) { visible -> if (visible) 1f else 0.8f }

    val rainbowColor = getAnimatedRainbowColor()

    if (alpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = when (feedback.type) {
                FeedbackType.BRIGHTNESS -> Alignment.CenterStart
                FeedbackType.VOLUME -> Alignment.CenterEnd
                else -> Alignment.Center
            }
        ) {
            when (feedback.type) {
                FeedbackType.PLAY_PAUSE -> {
                    Icon(
                        imageVector = feedback.icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(80.dp)
                            .scale(scale)
                            .alpha(alpha)
                    )
                }
                FeedbackType.BRIGHTNESS, FeedbackType.VOLUME -> {
                    // Vertical slider style
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(12.dp)
                            .scale(scale)
                            .alpha(alpha)
                    ) {
                        Icon(feedback.icon, contentDescription = null, tint = feedback.color)
                        Spacer(modifier = Modifier.height(8.dp))
                        feedback.value?.let { value ->
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(value.coerceIn(0f, 1f))
                                        .background(rainbowColor) // Using animated rainbow color
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = feedback.text,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {
                    // Standard Center Chip (Speed, Seek, Zoom, Lock)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .scale(scale)
                            .alpha(alpha)
                    ) {
                        Icon(feedback.icon, contentDescription = null, tint = feedback.color, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = feedback.text,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
