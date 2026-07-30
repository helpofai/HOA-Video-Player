package com.helpofai.videoplayer.tools.vault.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Enterprise Grade Metadata Tracking.
 * Instead of storing hidden file names in an unencrypted Room database (which exposes hidden file 
 * names to anyone who roots the device), this metadata object will be serialized to JSON and 
 * encrypted alongside the files themselves in the vault.
 */
data class VaultFileMetadata(
    val id: String, // Secure randomly generated ID matching the encrypted file name
    val originalName: String,
    val originalPath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateVaultedMs: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("originalName", originalName)
        put("originalPath", originalPath)
        put("mimeType", mimeType)
        put("sizeBytes", sizeBytes)
        put("dateVaultedMs", dateVaultedMs)
    }

    companion object {
        fun fromJson(json: JSONObject): VaultFileMetadata = VaultFileMetadata(
            id = json.getString("id"),
            originalName = json.getString("originalName"),
            originalPath = json.getString("originalPath"),
            mimeType = json.getString("mimeType"),
            sizeBytes = json.getLong("sizeBytes"),
            dateVaultedMs = json.getLong("dateVaultedMs")
        )
    }
}

data class VaultIndex(
    val files: List<VaultFileMetadata> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        val array = JSONArray()
        files.forEach { array.put(it.toJson()) }
        put("files", array)
    }

    companion object {
        fun fromJson(json: JSONObject): VaultIndex {
            val array = json.getJSONArray("files")
            val list = mutableListOf<VaultFileMetadata>()
            for (i in 0 until array.length()) {
                list.add(VaultFileMetadata.fromJson(array.getJSONObject(i)))
            }
            return VaultIndex(list)
        }
    }
}
