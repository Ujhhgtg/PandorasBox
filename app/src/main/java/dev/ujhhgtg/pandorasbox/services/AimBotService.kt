package dev.ujhhgtg.pandorasbox.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.receivers.StopServiceReceiver
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import dev.ujhhgtg.pandorasbox.utils.ServiceLocator
import dev.ujhhgtg.pandorasbox.utils.settings.PrefsRepository

class AimBotService : Service() {
    private lateinit var settings: PrefsRepository

    companion object {
        @Volatile
        var isRunning: MutableState<Boolean> = mutableStateOf(false)
        const val TAG: String = "PB.AimBotService"
        const val ID: Int = 2
    }

    override fun onCreate() {
        super.onCreate()

        if (!PermissionManager.checkNotifications(this) ||
            !PermissionManager.checkOverlay(this)
        ) {
            Log.d(TAG, "required permissions are not granted")
            throw IllegalAccessException("required permissions are not granted")
        }

        isRunning.value = true
        startForegroundServiceWithNotification()
        settings = PrefsRepository(this)
        ServiceLocator.register(this)
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
        stopIntent.putExtra("service", ID)
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

        startForeground(ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()

        isRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}