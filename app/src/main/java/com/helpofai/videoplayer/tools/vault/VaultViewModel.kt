package com.helpofai.videoplayer.tools.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpofai.videoplayer.tools.vault.crypto.VaultCryptoManager
import com.helpofai.videoplayer.tools.vault.data.VaultFileMetadata
import com.helpofai.videoplayer.tools.vault.data.VaultIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultCryptoManager: VaultCryptoManager
) : ViewModel() {

    private val _vaultedFiles = MutableStateFlow<List<VaultFileMetadata>>(emptyList())
    val vaultedFiles: StateFlow<List<VaultFileMetadata>> = _vaultedFiles

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // In-memory decrypted temp files to clean up
    private val activeDecryptedFiles = mutableListOf<File>()

    private val _cryptoProgress = MutableStateFlow(0f)
    val cryptoProgress: StateFlow<Float> = _cryptoProgress

    private val _cryptoTaskName = MutableStateFlow("")
    val cryptoTaskName: StateFlow<String> = _cryptoTaskName

    init {
        loadVaultIndex()
    }

    private fun loadVaultIndex() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // In an enterprise system, the index itself would be encrypted.
                // For simplicity here, we assume it's read/decrypted properly.
                // You would use vaultCryptoManager to decrypt the index file if it exists.
                // For this implementation, we will mock the index load if no file exists yet.
                // Note: The godmode response detailed that the index IS encrypted. We will build that!
                val indexResult = vaultCryptoManager.readEncryptedIndex()
                if (indexResult.isSuccess) {
                    _vaultedFiles.value = indexResult.getOrNull()?.files ?: emptyList()
                } else {
                    _vaultedFiles.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load vault index"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun hasPinSetup(): Boolean = vaultCryptoManager.hasPinSetup()

    fun setupPin(pin: String) {
        vaultCryptoManager.setupPin(pin)
    }

    fun verifyPin(pin: String): Boolean {
        return vaultCryptoManager.verifyPin(pin)
    }
    fun encryptFileToVault(uri: Uri, deleteOriginal: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _cryptoTaskName.value = "Encrypting File"
            _cryptoProgress.value = 0f
            val result = vaultCryptoManager.encryptFile(uri, deleteOriginal) { progress ->
                _cryptoProgress.value = progress
            }
            if (result.isSuccess) {
                val encryptedFile = result.getOrNull()
                if (encryptedFile != null) {
                    // Update index
                    val newMeta = VaultFileMetadata(
                        id = encryptedFile.name,
                        originalName = vaultCryptoManager.getFileName(uri) ?: "Unknown File",
                        originalPath = uri.path ?: "",
                        mimeType = "video/*", // Should resolve from content resolver
                        sizeBytes = encryptedFile.length(),
                        dateVaultedMs = System.currentTimeMillis()
                    )
                    val updatedList = _vaultedFiles.value.toMutableList().apply { add(newMeta) }
                    _vaultedFiles.value = updatedList
                    
                    // Save encrypted index
                    vaultCryptoManager.saveEncryptedIndex(VaultIndex(updatedList))
                }
            } else {
                _error.value = "Encryption failed: ${result.exceptionOrNull()?.message}"
            }
            _cryptoProgress.value = 0f
            _cryptoTaskName.value = ""
            _isLoading.value = false
        }
    }

    fun decryptForViewing(metadata: VaultFileMetadata, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _cryptoTaskName.value = "Decrypting for Viewing"
            _cryptoProgress.value = 0f
            val encryptedFile = File(vaultCryptoManager.vaultDirectory, metadata.id)
            if (encryptedFile.exists()) {
                val result = vaultCryptoManager.decryptToTempCache(encryptedFile) { progress ->
                    _cryptoProgress.value = progress
                }
                if (result.isSuccess) {
                    val tempFile = result.getOrNull()
                    if (tempFile != null) {
                        activeDecryptedFiles.add(tempFile)
                        onReady(tempFile)
                    }
                } else {
                    _error.value = "Failed to unlock file for viewing"
                }
            }
            _cryptoProgress.value = 0f
            _cryptoTaskName.value = ""
            _isLoading.value = false
        }
    }
    
    fun deleteFromVault(metadata: VaultFileMetadata) {
        viewModelScope.launch {
            val encryptedFile = File(vaultCryptoManager.vaultDirectory, metadata.id)
            if (encryptedFile.exists()) {
                encryptedFile.delete()
            }
            val updatedList = _vaultedFiles.value.filter { it.id != metadata.id }
            _vaultedFiles.value = updatedList
            vaultCryptoManager.saveEncryptedIndex(VaultIndex(updatedList))
        }
    }

    fun exportFileFromVault(metadata: VaultFileMetadata, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _cryptoTaskName.value = "Restoring File"
            _cryptoProgress.value = 0f
            val encryptedFile = File(vaultCryptoManager.vaultDirectory, metadata.id)
            if (encryptedFile.exists()) {
                val result = vaultCryptoManager.exportFileToDownloads(
                    encryptedFile, 
                    metadata.originalName, 
                    metadata.mimeType
                ) { progress ->
                    _cryptoProgress.value = progress
                }
                if (result.isSuccess) {
                    // Remove from vault since it's restored
                    deleteFromVault(metadata)
                    withContext(Dispatchers.Main) { onResult(true) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false) }
                }
            } else {
                withContext(Dispatchers.Main) { onResult(false) }
            }
            _cryptoProgress.value = 0f
            _cryptoTaskName.value = ""
            _isLoading.value = false
        }
    }

    fun cleanup() {
        activeDecryptedFiles.forEach { file ->
            vaultCryptoManager.secureDeleteTempFile(file)
        }
        activeDecryptedFiles.clear()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
    
    fun secureDeleteTempFile(tempFile: File) {
        vaultCryptoManager.secureDeleteTempFile(tempFile)
        activeDecryptedFiles.remove(tempFile)
    }
}
