package com.helpofai.videoplayer.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSheet(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismissRequest: () -> Unit
) {
    var speed by remember { mutableFloatStateOf(currentSpeed) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Playback Speed: ${String.format("%.1fx", speed)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Slider(
                value = speed,
                onValueChange = { 
                    speed = (it * 10.0f).roundToInt() / 10.0f
                    onSpeedSelected(speed)
                },
                valueRange = 0.1f..4.0f,
                steps = 38
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedChip("0.5x", 0.5f, speed) { 
                    speed = 0.5f
                    onSpeedSelected(0.5f)
                }
                SpeedChip("1.0x", 1.0f, speed) { 
                    speed = 1.0f
                    onSpeedSelected(1.0f)
                }
                SpeedChip("1.5x", 1.5f, speed) { 
                    speed = 1.5f
                    onSpeedSelected(1.5f)
                }
                SpeedChip("2.0x", 2.0f, speed) { 
                    speed = 2.0f
                    onSpeedSelected(2.0f)
                }
            }
        }
    }
}

@Composable
fun SpeedChip(label: String, targetSpeed: Float, currentSpeed: Float, onClick: () -> Unit) {
    val isSelected = targetSpeed == currentSpeed
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) }
    )
}
