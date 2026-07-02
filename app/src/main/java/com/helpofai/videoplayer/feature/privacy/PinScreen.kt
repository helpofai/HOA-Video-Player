package com.helpofai.videoplayer.feature.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.core.data.PrivacyRepository

@Composable
fun PinScreen(
    privacyRepository: PrivacyRepository,
    onSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    val savedPin = remember { privacyRepository.getPin() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enter Privacy PIN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // PIN Dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 0 until 4) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < enteredPin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
        
        if (isError) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Incorrect PIN", color = MaterialTheme.colorScheme.error)
        } else {
            Spacer(modifier = Modifier.height(36.dp))
        }

        // Numpad
        val keypad = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        keypad.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    if (key.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                isError = false
                                if (key == "DEL") {
                                    if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                } else {
                                    if (enteredPin.length < 4) {
                                        enteredPin += key
                                        if (enteredPin.length == 4) {
                                            if (enteredPin == savedPin) {
                                                onSuccess()
                                            } else {
                                                isError = true
                                                enteredPin = ""
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(80.dp)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = key,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(80.dp))
                    }
                }
            }
        }
    }
}
