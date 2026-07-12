package com.helpofai.videoplayer.feature.workspace.transfers

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun NetworkClientView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    
    val savedConnections = remember {
        mutableStateListOf(
            NetworkConnection("FTP Server (192.168.1.15)", "FTP", "192.168.1.15", "21", "Connected"),
            NetworkConnection("SFTP Server (Mainframe)", "SFTP", "sftp.helpofai.com", "22", "SSH Secure"),
            NetworkConnection("SMB Share (Home NAS)", "SMB", "192.168.1.50", "445", "Active Share"),
            NetworkConnection("WebDAV (NextCloud)", "WebDAV", "cloud.helpofai.com", "443", "Sync Ready"),
            NetworkConnection("USB OTG Drive (Cruzer)", "USB-OTG", "/mnt/media_rw/usbhost", "0", "Mounted")
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Network & Mounting",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Channel", tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Text(
            text = "Connect directly to remote servers, file shares, and mounted USB OTG devices.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        
        savedConnections.forEach { connection ->
            NetworkItemRow(
                connection = connection,
                onClick = {
                    Toast.makeText(context, "Browsing directory: ${connection.name}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
    
    if (showAddDialog) {
        AddConnectionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newConn ->
                savedConnections.add(newConn)
                showAddDialog = false
                Toast.makeText(context, "Added connection: ${newConn.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

data class NetworkConnection(
    val name: String,
    val protocol: String,
    val address: String,
    val port: String,
    val status: String
)

@Composable
private fun NetworkItemRow(
    connection: NetworkConnection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E222B).copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(connection.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Protocol: ${connection.protocol}  •  ${connection.address}:${connection.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(connection.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AddConnectionDialog(
    onDismiss: () -> Unit,
    onSave: (NetworkConnection) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("FTP") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("21") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val protocols = listOf("FTP", "SFTP", "SMB", "WebDAV", "USB-OTG")
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF161A22),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Network Channel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }
                
                // Protocol Dropdown simulated selector
                Text("Select Protocol", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    protocols.forEach { proto ->
                        val selected = protocol == proto
                        Surface(
                            modifier = Modifier
                                .clickable { 
                                    protocol = proto
                                    port = when (proto) {
                                        "FTP" -> "21"
                                        "SFTP" -> "22"
                                        "SMB" -> "445"
                                        "WebDAV" -> "443"
                                        else -> "0"
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f))
                        ) {
                            Text(
                                text = proto,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.Black else Color.LightGray
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Connection Name (e.g. Home Router)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Server Address / Domain IP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (protocol != "USB-OTG") {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        if (name.isNotBlank() && address.isNotBlank()) {
                            onSave(NetworkConnection(name, protocol, address, port, "Active"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect Channel", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
