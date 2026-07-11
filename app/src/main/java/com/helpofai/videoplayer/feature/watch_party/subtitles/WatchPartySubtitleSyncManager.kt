package com.helpofai.videoplayer.feature.watch_party.subtitles

class WatchPartySubtitleSyncManager {
    fun getSubtitleSyncState(delayMs: Int, language: String): Pair<Int, String> {
        return delayMs to language
    }
}
