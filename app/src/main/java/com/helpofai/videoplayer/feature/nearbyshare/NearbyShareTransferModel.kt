package com.helpofai.videoplayer.feature.nearbyshare

enum class NearbyShareRole {
    SENDER, RECEIVER
}

enum class NearbyShareStatus {
    IDLE,
    DISCOVERING,
    ADVERTISING,
    CONNECTING,
    TRANSFERRING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class NearbyShareDevice(
    val name: String,
    val ipAddress: String,
    val port: Int,
    val isSender: Boolean,
    val lastSeen: Long = System.currentTimeMillis()
)

data class NearbyShareTransferState(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val bytesTransferred: Long,
    val speedBytesPerSecond: Long,
    val status: NearbyShareStatus,
    val role: NearbyShareRole,
    val peerDeviceName: String,
    val error: String? = null,
    val isPaused: Boolean = false,
    val filePath: String? = null
) {
    val progress: Float
        get() = if (fileSize > 0) bytesTransferred.toFloat() / fileSize else 0f

    val formattedProgress: String
        get() = String.format("%.0f%%", progress * 100)

    val formattedSpeed: String
        get() {
            val speedKb = speedBytesPerSecond / 1024.0
            val speedMb = speedKb / 1024.0
            return when {
                speedMb >= 1.0 -> String.format("%.1f MB/s", speedMb)
                speedKb >= 1.0 -> String.format("%.1f KB/s", speedKb)
                else -> "${speedBytesPerSecond} B/s"
            }
        }

    val formattedEta: String
        get() {
            if (isPaused || speedBytesPerSecond <= 0) return "Paused"
            val remainingBytes = fileSize - bytesTransferred
            if (remainingBytes <= 0) return "0s"
            val etaSeconds = remainingBytes / speedBytesPerSecond
            val hours = etaSeconds / 3600
            val minutes = (etaSeconds % 3600) / 60
            val seconds = etaSeconds % 60
            return if (hours > 0) {
                String.format("%dh %dm", hours, minutes)
            } else if (minutes > 0) {
                String.format("%dm %ds", minutes, seconds)
            } else {
                "${seconds}s"
            }
        }
}
