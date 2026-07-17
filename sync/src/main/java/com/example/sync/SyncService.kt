package com.example.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "varotra_sync_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_START_SERVER = "com.example.sync.ACTION_START_SERVER"
        const val ACTION_START_CLIENT = "com.example.sync.ACTION_START_CLIENT"
        const val ACTION_STOP = "com.example.sync.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_SERVER -> {
                startForeground(NOTIFICATION_ID, createNotification("Mandeha ny Server (VAROTRA Sync)..."))
            }
            ACTION_START_CLIENT -> {
                val host = intent.getStringExtra("EXTRA_HOST") ?: "127.0.0.1"
                startForeground(NOTIFICATION_ID, createNotification("Mampifandray amin'ny Server ($host)..."))
            }
            ACTION_STOP -> {
                SyncManager.stopAll()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotification(contentText: String): Notification {
        // Safe check for class exists, or use default launcher intent
        val pm = packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VAROTRA - Fampidirana data mitohy")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VAROTRA Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mampifandray mivantana ny fitaovana VAROTRA"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
