package com.helpofai.videoplayer.core.playback

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var videoPlayer: VideoPlayer

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val sessionActivityPendingIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
            android.app.PendingIntent.getActivity(this, 0, sessionIntent, android.app.PendingIntent.FLAG_IMMUTABLE)
        }
        
        val builder = MediaSession.Builder(this, videoPlayer.player)
        sessionActivityPendingIntent?.let {
            builder.setSessionActivity(it)
        }
        mediaSession = builder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }
}
