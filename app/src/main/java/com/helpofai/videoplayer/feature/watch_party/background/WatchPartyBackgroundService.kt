package com.helpofai.videoplayer.feature.watch_party.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.helpofai.videoplayer.MainActivity

class WatchPartyBackgroundService : Service() {

    companion object {
        const val CHANNEL_ID      = "watch_party_bg_channel"
        const val NOTIFICATION_ID = 8877
        const val ACTION_STOP     = "WATCH_PARTY_STOP"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, WatchPartyBackgroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WatchPartyBackgroundService::class.java))
        }
    }

    override fun onCreate() { super.onCreate(); createChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // End the active session and disconnect when stopped via notification
            com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager.getInstance().endSession()
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, WatchPartyBackgroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Watch Party Active")
            .setContentText("Room is running. Tap to return.")
            .setSubText("HOA Video Player")
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop Session", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val chan = NotificationChannel(CHANNEL_ID, "Watch Party Background", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Keeps Watch Party alive in background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(chan)
    }
}
