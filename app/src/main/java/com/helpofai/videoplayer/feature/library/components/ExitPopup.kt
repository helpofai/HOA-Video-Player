package com.helpofai.videoplayer.feature.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.helpofai.videoplayer.core.ads.NativeAdCard

@Composable
fun ExitPopup(
    onDismiss: () -> Unit,
    onBackground: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Leaving so soon?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check out this ad before you go, or let the app run in the background.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Native Ad for Revenue Boost
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp)) {
                        NativeAdCard(modifier = Modifier.fillMaxWidth())
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text("No", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onBackground,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Background", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Yes, Exit App", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
