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
import androidx.core.content.FileProvider
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.receivers.StopServiceReceiver
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import dev.ujhhgtg.pandorasbox.utils.ServiceLocator
import dev.ujhhgtg.pandorasbox.utils.settings.PrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

class DownloadService: Service() {
    private lateinit var prefs: PrefsRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationIdCounter = AtomicInteger(ID + 1)

    companion object {
        @Volatile
        var isRunning: MutableState<Boolean> = mutableStateOf(false)
        const val TAG: String = "PB.DownloadService"
        const val ID: Int = 5
        const val CHANNEL_ID = "download_channel"
    }

    override fun onCreate() {
        super.onCreate()

        if (!PermissionManager.checkNotifications(this)) {
            Log.d(TAG, "required permissions are not granted")
            throw IllegalAccessException("required permissions are not granted")
        }

        isRunning.value = true
        startForegroundServiceWithNotification()
        prefs = PrefsRepository(this)
        ServiceLocator.register(this)
    }

    private fun startForegroundServiceWithNotification() {
        val channelName = "Download Notification Channel"

        val channel = NotificationChannel(
            CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW
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
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Active")
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

    fun startDownload(
        url: String,
        outputDir: File,
        fileName: String,
        mimeType: String? = null,
        headers: Map<String, String> = emptyMap(),
        onProgress: ((Int) -> Unit)? = null,
        onComplete: ((File?) -> Unit)? = null
    ) {
        val notificationId = notificationIdCounter.getAndIncrement()

        scope.launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection

                for ((key, value) in headers) {
                    connection.setRequestProperty(key, value)
                }

                connection.connect()

                val length = connection.contentLength
                val input = connection.inputStream

                val outputFile = File(outputDir, fileName)
                val output = FileOutputStream(outputFile)

                val buffer = ByteArray(8 * 1024)
                var total: Long = 0
                var count: Int
                while (input.read(buffer).also { count = it } != -1) {
                    output.write(buffer, 0, count)
                    total += count

                    if (length > 0) {
                        val progress = (total * 100 / length).toInt()
                        onProgress?.invoke(progress)
                        updateNotification(notificationId, fileName, progress)
                    }
                }

                output.flush()
                output.close()
                input.close()

                showCompleteNotification(notificationId, fileName, outputFile, mimeType)
                removeNotification(notificationId)

                onComplete?.invoke(outputFile)
            } catch (e: Exception) {
                Log.e(TAG, "exception while downloading", e)
                showFailedNotification(notificationId, fileName)
                removeNotification(notificationId)
                onComplete?.invoke(null)
            }
        }
    }

    private fun removeNotification(notificationId: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }

    private fun updateNotification(notificationId: Int, fileName: String, progress: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.downloading_file))
            .setContentText("$fileName ($progress%)")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        manager.notify(notificationId, notification)
    }

    private fun showCompleteNotification(notificationId: Int, fileName: String, file: File, mimeType: String? = null) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            val fileUri = FileProvider.getUriForFile(
                this@DownloadService,
                "${packageName}.fileprovider",
                file
            )
            setDataAndType(fileUri, mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download complete")
            .setContentText(fileName)
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId, notification)
    }

    private fun showFailedNotification(notificationId: Int, fileName: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download failed")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId, notification)
    }

    override fun onDestroy() {
        super.onDestroy()

        isRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}