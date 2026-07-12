package com.helpofai.videoplayer.feature.watch_party.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class WatchPartySecurityManager {
    
    fun generateSecureSessionPassword(): String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..6)
            .map { charset.random() }
            .joinToString("")
    }

    private fun deriveKey(key: String): SecretKeySpec {
        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
        val keyBytes = sha256.digest(key.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptSessionPayload(payload: String, key: String): String {
        return try {
            val secretKey = deriveKey(key)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
            val ciphertext = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
            
            // Combine IV and ciphertext: first 12 bytes are IV
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            payload // Fallback to plain text on encryption failure
        }
    }

    fun decryptSessionPayload(encryptedPayload: String, key: String): String {
        return try {
            val secretKey = deriveKey(key)
            val combined = Base64.decode(encryptedPayload, Base64.NO_WRAP)
            if (combined.size < 12) return encryptedPayload
            
            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            val ciphertext = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, ciphertext, 0, ciphertext.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            encryptedPayload // Fallback on decryption failure
        }
    }
}
