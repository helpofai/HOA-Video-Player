package com.helpofai.videoplayer.feature.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlayerTopToolbar(
    isVisible: Boolean,
    title: String,
    onBackClick: () -> Unit,
    onLockClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onEqClick: () -> Unit,
    onLoopClick: () -> Unit,
    onInfoClick: () -> Unit,
    onRotateClick: () -> Unit,
    onVideoAdjustmentsClick: () -> Unit,
    abRepeatState: String,
    onABRepeatClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onScreenshotClick: () -> Unit,
    onMoreClick: () -> Unit,
    decoderMode: String = "HW",
    onDecoderClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(180)
        ) + fadeIn(animationSpec = tween(180)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(150)
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        var isToolsExpanded by remember { mutableStateOf(false) }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                // Add pointerInput to prevent clicks from passing through the toolbar background
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { /* Consume tap so it doesn't dismiss the UI */ }
                    )
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                if (!isToolsExpanded) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(iterations = Int.MAX_VALUE)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onLockClick) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White)
                        }
                        IconButton(onClick = onSpeedClick) {
                            Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                        }
                        IconButton(onClick = onEqClick) {
                            Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer", tint = Color.White)
                        }
                        IconButton(onClick = onLoopClick) {
                            Icon(Icons.Default.Repeat, contentDescription = "Loop", tint = Color.White)
                        }
                        IconButton(onClick = onAudioClick) {
                            Icon(Icons.Default.Audiotrack, contentDescription = "Audio", tint = Color.White)
                        }
                        IconButton(onClick = onSubtitlesClick) {
                            Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                        }
                        IconButton(onClick = onScreenshotClick) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Screenshot", tint = Color.White)
                        }
                        Box(modifier = Modifier.clickable { onABRepeatClick() }.padding(8.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SyncAlt, contentDescription = "AB Repeat", tint = if (abRepeatState.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.White)
                            if (abRepeatState.isNotEmpty()) {
                                Text(
                                    text = abRepeatState,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }
                        IconButton(onClick = onRotateClick) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = Color.White)
                        }
                        IconButton(onClick = onVideoAdjustmentsClick) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Adjustments", tint = Color.White)
                        }
                        val context = androidx.compose.ui.platform.LocalContext.current
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            IconButton(onClick = {
                                val activity = context as? android.app.Activity
                                val params = android.app.PictureInPictureParams.Builder()
                                    .setAspectRatio(android.util.Rational(16, 9))
                                    .build()
                                activity?.enterPictureInPictureMode(params)
                            }) {
                                Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Mini Player", tint = Color.White)
                            }
                        }
                        IconButton(onClick = onInfoClick) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                        }
                    }
                }

                // Decoder Mode Selector (HW, HW+, SW)
                Text(
                    text = decoderMode,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onDecoderClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Expand/Collapse Tools Toggle
                IconButton(onClick = { isToolsExpanded = !isToolsExpanded }) {
                    Icon(
                        if (isToolsExpanded) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = if (isToolsExpanded) "Collapse Tools" else "Expand Tools",
                        tint = Color.White
                    )
                }

                Box {
                    IconButton(onClick = onMoreClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                }
            }
        }
    }
}
