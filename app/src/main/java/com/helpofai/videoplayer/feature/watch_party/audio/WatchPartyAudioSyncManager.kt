package com.helpofai.videoplayer.feature.watch_party.audio

class WatchPartyAudioSyncManager {
    fun getAudioTrackSyncState(audioChannelIndex: Int, codecType: String): Pair<Int, String> {
        return audioChannelIndex to codecType
    }
}
