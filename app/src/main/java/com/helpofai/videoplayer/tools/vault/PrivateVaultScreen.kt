package com.helpofai.videoplayer.tools.vault

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.helpofai.videoplayer.core.theme.frostedGlass
import com.helpofai.videoplayer.tools.vault.VaultViewModel
import com.helpofai.videoplayer.tools.vault.data.VaultFileMetadata
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation

enum class VaultAuthState {
    SETUP_PIN, ENTER_PIN, AUTHENTICATED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateVaultScreen(
    onNavigateUp: () -> Unit,
    onPlayVideo: (String) -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var authState by remember { 
        mutableStateOf(if (viewModel.hasPinSetup()) VaultAuthState.ENTER_PIN else VaultAuthState.SETUP_PIN) 
    }
    var pinError by remember { mutableStateOf<String?>(null) }
    
    val canAuthenticateBiometric = remember {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    val vaultedFiles by viewModel.vaultedFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val cryptoTaskName by viewModel.cryptoTaskName.collectAsState()
    val cryptoProgress by viewModel.cryptoProgress.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.encryptFileToVault(uri, deleteOriginal = true)
        }
    }

    val authenticate = {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(
                fragmentActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode != BiometricPrompt.ERROR_CANCELED && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                            Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        authState = VaultAuthState.AUTHENTICATED
                        pinError = null
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Private Vault")
                .setSubtitle("Use your fingerprint or device PIN to access hidden files")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            Toast.makeText(context, "Biometric authentication not supported on this context", Toast.LENGTH_SHORT).show()
        }
    }

    // Trigger biometric auth on first composition if PIN is already set up and biometric is available
    LaunchedEffect(Unit) {
        if (authState == VaultAuthState.ENTER_PIN && canAuthenticateBiometric) {
            authenticate()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private Vault", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(targetState = authState, label = "VaultAuth") { state ->
                when (state) {
                    VaultAuthState.AUTHENTICATED -> {
                        VaultContent(
                            vaultedFiles = vaultedFiles,
                            isLoading = isLoading,
                            onImportClick = { filePickerLauncher.launch("*/*") },
                            onFileClick = { file ->
                                viewModel.decryptForViewing(file) { tempFile ->
                                    onPlayVideo(tempFile.absolutePath)
                                }
                            },
                            onDeleteClick = { file -> viewModel.deleteFromVault(file) },
                            onRestoreClick = { file -> 
                                viewModel.exportFileFromVault(file) { success ->
                                    if (success) {
                                        Toast.makeText(context, "File restored to Downloads", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to restore file", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                    VaultAuthState.SETUP_PIN -> {
                        VaultPinSetup(onPinSet = { pin ->
                            viewModel.setupPin(pin)
                            authState = VaultAuthState.AUTHENTICATED
                        })
                    }
                    VaultAuthState.ENTER_PIN -> {
                        VaultPinEntry(
                            onPinEntered = { pin ->
                                if (viewModel.verifyPin(pin)) {
                                    authState = VaultAuthState.AUTHENTICATED
                                    pinError = null
                                } else {
                                    pinError = "Incorrect PIN"
                                }
                            },
                            onUseBiometric = authenticate,
                            error = pinError,
                            biometricAvailable = canAuthenticateBiometric
                        )
                    }
                }
            }
        }
        
        if (cryptoTaskName.isNotEmpty()) {
            DraggableProgressPopup(
                taskName = cryptoTaskName,
                progress = cryptoProgress
            )
        }
    }
}

@Composable
fun DraggableProgressPopup(taskName: String, progress: Float) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = false)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        offsetY += delta
                    }
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX += delta
                    }
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = taskName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(0.8f).height(8.dp),
                    color = Color(0xFF00CEC9),
                    trackColor = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Drag to move",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun VaultLockedState(onUnlockClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF00CEC9).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(50.dp),
                tint = Color(0xFF00CEC9)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Vault is Locked",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Authenticate to view encrypted files.",
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onUnlockClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CEC9))
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unlock Now", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VaultContent(
    vaultedFiles: List<VaultFileMetadata>,
    isLoading: Boolean,
    onImportClick: () -> Unit,
    onFileClick: (VaultFileMetadata) -> Unit,
    onDeleteClick: (VaultFileMetadata) -> Unit,
    onRestoreClick: (VaultFileMetadata) -> Unit
) {
    if (isLoading && vaultedFiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFF39C12))
        }
        return
    }

    if (vaultedFiles.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Vault Empty",
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Vault is Empty",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Files encrypted here will be hidden from other apps.",
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onImportClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00CEC9))
            ) {
                Text("Import Files", color = Color(0xFF00CEC9))
            }
        }
    } else {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(vaultedFiles.size) { index ->
                val file = vaultedFiles[index]
                VaultFileItem(
                    file = file,
                    onClick = { onFileClick(file) },
                    onDelete = { onDeleteClick(file) },
                    onRestore = { onRestoreClick(file) }
                )
            }
        }
    }
}

@Composable
fun VaultFileItem(
    file: VaultFileMetadata,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
            .frostedGlass(cornerRadius = 12.dp, surfaceAlpha = 0.2f, surfaceColor = Color.Black),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF3B82F6))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.originalName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(file.dateVaultedMs)),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Lock, contentDescription = "Restore", tint = Color(0xFF00CEC9)) // Fallback icon for restore
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Lock, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f)) // Fallback icon for delete
            }
        }
    }
}
