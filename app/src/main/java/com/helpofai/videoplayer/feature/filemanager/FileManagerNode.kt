package com.helpofai.videoplayer.feature.filemanager

import java.io.File

data class FileManagerNode(
    val file: File?,
    val uriString: String? = null,
    val isDirectory: Boolean,
    val isExpanded: Boolean = false,
    val depth: Int = 0,
    val isVideo: Boolean = false,
    val isSubtitle: Boolean = false,
    val isAudio: Boolean = false,
    val childCount: Int = 0,
    val formattedSize: String = "",
    val isSelected: Boolean = false,
    // Custom parameters for SAF document mapping
    val customName: String = "",
    val customExtension: String = "",
    val customPath: String = ""
) {
    val name: String
        get() = if (file != null) file.name.ifEmpty { file.absolutePath } else customName
        
    val extension: String
        get() = if (file != null) file.extension.lowercase() else customExtension
        
    val path: String
        get() = if (file != null) file.absolutePath else customPath
}
