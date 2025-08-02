package dev.ujhhgtg.pandorasbox.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.utils.SettingsRepository
import dev.ujhhgtg.pandorasbox.receivers.StopServiceReceiver
import dev.ujhhgtg.pandorasbox.utils.PermissionManager

class AimBotService: Service() {
    private lateinit var settings: SettingsRepository

    companion object {
        @Volatile
        var isRunning: MutableState<Boolean> = mutableStateOf(false)
    }

    override fun onCreate() {
        super.onCreate()

        if (!PermissionManager.checkNotifications(this) ||
            !PermissionManager.checkOverlay(this) ||
            !PermissionManager.checkShizuku()) {
            throw IllegalAccessException("required permissions are not granted")
        }

        isRunning.value = true
        startForegroundServiceWithNotification()
        settings = SettingsRepository(this)
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "aim_bot_channel"
        val channelName = "Aim Bot Notification Channel"

        val channel = NotificationChannel(
            channelId, channelName, NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val stopIntent = Intent(this, StopServiceReceiver::class.java)
        stopIntent.putExtra("service", 1)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Aim Bot Active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.cancel_24px,
                "Stop",
                stopPendingIntent
            )
            .setOngoing(true)
            .build()

        startForeground(2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()


    }

    override fun onBind(intent: Intent?): IBinder? = null
}