package com.helpofai.videoplayer.tools.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun VaultPinSetup(onPinSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00CEC9), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Setup Vault PIN", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Create a PIN to secure your vault.", color = Color.White.copy(alpha = 0.6f))
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it },
            label = { Text("Enter PIN (4-6 digits)", color = Color.White.copy(0.6f)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00CEC9), unfocusedBorderColor = Color.White.copy(0.3f)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { if (it.length <= 6) confirmPin = it },
            label = { Text("Confirm PIN", color = Color.White.copy(0.6f)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00CEC9), unfocusedBorderColor = Color.White.copy(0.3f)
            )
        )
        
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = Color.Red)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (pin.length < 4) error = "PIN must be at least 4 digits"
                else if (pin != confirmPin) error = "PINs do not match"
                else onPinSet(pin)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CEC9)),
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
        ) {
            Text("Save PIN", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VaultPinEntry(onPinEntered: (String) -> Unit, onUseBiometric: () -> Unit, error: String?, biometricAvailable: Boolean) {
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00CEC9), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Enter Vault PIN", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it },
            label = { Text("PIN", color = Color.White.copy(0.6f)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00CEC9), unfocusedBorderColor = Color.White.copy(0.3f)
            )
        )
        
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = Color.Red)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { onPinEntered(pin) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CEC9)),
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
        ) {
            Text("Unlock", fontWeight = FontWeight.Bold)
        }
        
        if (biometricAvailable) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onUseBiometric) {
                Text("Use Fingerprint / Face", color = Color(0xFF00CEC9))
            }
        }
    }
}
