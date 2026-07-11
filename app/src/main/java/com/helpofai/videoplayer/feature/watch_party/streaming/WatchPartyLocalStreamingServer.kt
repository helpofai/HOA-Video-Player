package com.helpofai.videoplayer.feature.watch_party.streaming

import android.content.Context
import com.helpofai.videoplayer.core.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WatchPartyLocalStreamingServer(private val context: Context) {
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning
    
    fun startStreamingServer(video: Video, port: Int = 8080): String {
        _isServerRunning.value = true
        // Simulates host starting local streaming socket engine mapping local files to URI paths
        return "http://192.168.1.100:$port/stream/${video.id}"
    }
    
    fun stopStreamingServer() {
        _isServerRunning.value = false
    }
}
