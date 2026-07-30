package com.helpofai.videoplayer.tools.vault.crypto

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class VaultCryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val vaultDirectory = File(context.filesDir, "private_vault").apply {
        if (!exists()) mkdirs()
    }

    private val indexFile = File(vaultDirectory, "vault_index.enc")

    // Creates the MasterKey for AES-256-GCM encryption, tied to Android Keystore.
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPrefs by lazy {
        androidx.security.crypto.EncryptedSharedPreferences.create(
            context,
            "vault_secure_prefs",
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasPinSetup(): Boolean {
        return sharedPrefs.contains("vault_pin")
    }

    fun setupPin(pin: String) {
        sharedPrefs.edit().putString("vault_pin", pin).apply()
    }

    fun verifyPin(pin: String): Boolean {
        return sharedPrefs.getString("vault_pin", null) == pin
    }

    /**
     * Encrypts ANY file type (video, image, document) into the vault.
     * @param sourceUri The URI of the file to encrypt.
     * @param deleteOriginal If true, attempts to securely delete the original file.
     * @return The encrypted File object stored in internal storage.
     */
    suspend fun encryptFile(sourceUri: Uri, deleteOriginal: Boolean = false, onProgress: (Float) -> Unit = {}): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(sourceUri) ?: "vault_file_${System.currentTimeMillis()}"
            val extension = fileName.substringAfterLast('.', "")
            
            // Generate a secure random ID for the encrypted file to obscure its original name
            val secureRandomName = generateSecureId() + if (extension.isNotEmpty()) ".$extension" else ""
            val encryptedFileDest = File(vaultDirectory, secureRandomName)

            val encryptedFile = EncryptedFile.Builder(
                context,
                encryptedFileDest,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { fd ->
                val totalBytes = fd.statSize.toFloat()
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    encryptedFile.openFileOutput().use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalBytes > 0) {
                                onProgress(totalRead / totalBytes)
                            }
                        }
                    }
                } ?: return@withContext Result.failure(Exception("Could not open input stream for URI: $sourceUri"))
            } ?: return@withContext Result.failure(Exception("Could not open file descriptor for URI: $sourceUri"))

            // Note: Scoped storage restrictions may prevent deleting original file via File API.
            // In a full implementation, you would use DocumentFile or MediaStore to delete.
            if (deleteOriginal) {
                // Best effort delete
                try {
                    val docFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, sourceUri)
                    if (docFile != null && docFile.exists()) {
                        docFile.delete()
                    } else {
                        val originalFile = File(sourceUri.path ?: "")
                        if (originalFile.exists()) {
                            originalFile.delete()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Result.success(encryptedFileDest)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Decrypts a vaulted file into a temporary cache directory for immediate viewing.
     * @param encryptedFile The vaulted, encrypted file.
     * @return The temporarily decrypted File in the cache directory.
     */
    suspend fun decryptToTempCache(encryptedFile: File, onProgress: (Float) -> Unit = {}): Result<File> = withContext(Dispatchers.IO) {
        try {
            val encryptedFileWrapper = EncryptedFile.Builder(
                context,
                encryptedFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val tempDecryptedFile = File(context.cacheDir, "decrypted_temp_${encryptedFile.name}")
            
            val totalBytes = encryptedFile.length().toFloat()
            encryptedFileWrapper.openFileInput().use { inputStream ->
                FileOutputStream(tempDecryptedFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress(totalRead / totalBytes)
                        }
                    }
                }
            }
            Result.success(tempDecryptedFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Decrypts a vaulted file and exports it to the device's Downloads folder.
     */
    suspend fun exportFileToDownloads(encryptedFile: File, originalName: String, mimeType: String, onProgress: (Float) -> Unit = {}): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val encryptedFileWrapper = EncryptedFile.Builder(
                context,
                encryptedFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, originalName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/VidPlayVault")
                }
            }
            
            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI // Fallback
            }

            val uri = context.contentResolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            val totalBytes = encryptedFile.length().toFloat()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                encryptedFileWrapper.openFileInput().use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            onProgress(totalRead / totalBytes)
                        }
                    }
                }
            } ?: return@withContext Result.failure(Exception("Could not open output stream for URI"))

            Result.success(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * MUST be called to aggressively securely delete the temp decrypted file after viewing.
     */
    fun secureDeleteTempFile(tempFile: File) {
        if (tempFile.exists()) {
            // Overwrite with zeros before deleting to prevent recovery
            try {
                val length = tempFile.length()
                val raf = java.io.RandomAccessFile(tempFile, "rw")
                raf.channel.force(true)
                // Write zero chunks
                val zeros = ByteArray(8192)
                var written: Long = 0
                while (written < length) {
                    raf.write(zeros)
                    written += zeros.size
                }
                raf.close()
            } catch (e: Exception) {
                // Ignore failure to overwrite, proceed to delete
            } finally {
                tempFile.delete()
            }
        }
    }

    fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result
    }

    suspend fun saveEncryptedIndex(index: com.helpofai.videoplayer.tools.vault.data.VaultIndex): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (indexFile.exists()) {
                indexFile.delete() // Overwrite
            }
            val encryptedFile = EncryptedFile.Builder(
                context,
                indexFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val jsonString = index.toJson().toString()
            encryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun readEncryptedIndex(): Result<com.helpofai.videoplayer.tools.vault.data.VaultIndex> = withContext(Dispatchers.IO) {
        try {
            if (!indexFile.exists()) {
                return@withContext Result.success(com.helpofai.videoplayer.tools.vault.data.VaultIndex())
            }
            val encryptedFile = EncryptedFile.Builder(
                context,
                indexFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val jsonString = encryptedFile.openFileInput().use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            }
            val jsonObject = org.json.JSONObject(jsonString)
            Result.success(com.helpofai.videoplayer.tools.vault.data.VaultIndex.fromJson(jsonObject))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun generateSecureId(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
