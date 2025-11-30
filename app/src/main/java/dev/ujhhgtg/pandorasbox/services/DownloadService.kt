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
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class DownloadService: Service() {
    private lateinit var prefs: PrefsRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dlIdCounter = AtomicInteger(ID + 1)

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

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.download_manager))
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
        val dlId = dlIdCounter.getAndIncrement()

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

                val startTime = System.currentTimeMillis()
                var lastTime = startTime
                var lastTotal: Long = 0

                while (input.read(buffer).also { count = it } != -1) {
                    output.write(buffer, 0, count)
                    total += count

                    if (length > 0) {
                        val now = System.currentTimeMillis()
                        if (now - lastTime >= 1000) { // update every second
                            val bytesThisSecond = total - lastTotal
                            val speed = bytesThisSecond * 1000 / (now - lastTime)
                            val remaining = (length - total) / (if (speed > 0) speed else 1)
                            val progress = (total * 100 / length).toInt()

                            onProgress?.invoke(progress)
                            updateNotification(dlId, fileName, progress, speed, remaining)

                            lastTime = now
                            lastTotal = total
                        }
                    }
                }

                output.flush()
                output.close()
                input.close()

                removeNotification(dlId)
                showCompleteNotification(dlId, fileName, outputFile, mimeType)

                onComplete?.invoke(outputFile)
            } catch (e: Exception) {
                Log.e(TAG, "exception while downloading", e)
                removeNotification(dlId)
                showFailedNotification(dlId, fileName)
                onComplete?.invoke(null)
            }
        }
    }

    private fun removeNotification(dlId: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(dlId)
    }

    private fun updateNotification(
        dlId: Int,
        fileName: String,
        progress: Int,
        speedBytes: Long,
        remainingSeconds: Long
    ) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val speedText = formatSpeed(speedBytes)
        val etaText = formatRemainingTime(remainingSeconds)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.downloading_file))
            .setContentText("$fileName • $speedText • $etaText left")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        manager.notify(dlId, notification)
    }

    private fun showCompleteNotification(dlId: Int, fileName: String, file: File, mimeType: String? = null) {
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
            dlId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.download_complete))
            .setContentText(fileName)
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        manager.notify(dlId, notification)
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

    private fun formatSpeed(bytesPerSecond: Long): String {
        val kbps = bytesPerSecond / 1024.0
        return if (kbps >= 1024) String.format(Locale.ROOT, "%.1f MiB/s", kbps / 1024) else String.format(Locale.ROOT, "%.0f KiB/s", kbps)
    }

    private fun formatRemainingTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
    }


    override fun onDestroy() {
        super.onDestroy()

        isRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}