package dev.ujhhgtg.pandorasbox.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.receivers.StopServiceReceiver
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import dev.ujhhgtg.pandorasbox.utils.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var settings: SettingsRepository

    private lateinit var dot: View
    private lateinit var vLine: View
    private lateinit var hLine: View

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var configJob: Job? = null
    private var currentPkg: String = ""

    companion object {
        @Volatile
        var isRunning: MutableState<Boolean> = mutableStateOf(false)
    }

    override fun onCreate() {
        super.onCreate()

        if (!PermissionManager.checkNotifications(this) ||
            !PermissionManager.checkOverlay(this) ||
            !PermissionManager.checkUsageStats(this)) {
            throw IllegalAccessException("required permissions are not granted")
        }

        isRunning.value = true
        startForegroundServiceWithNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settings = SettingsRepository(this)

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val initDotSize = 20
        val initLineWidth = 15

        dot = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(initDotSize, initDotSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFFFFFFF.toInt())
                setStroke(3, 0xFF000000.toInt())
            }
        }
        val dotParams = WindowManager.LayoutParams(
            initDotSize,
            initDotSize,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        )
        dotParams.gravity = Gravity.CENTER
        windowManager.addView(dot, dotParams)

        hLine = FrameLayout(this).apply {
            setBackgroundColor(0x80FFFFFF.toInt())
        }
        val horizontalParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            initLineWidth,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        )
        horizontalParams.gravity = Gravity.CENTER
        windowManager.addView(hLine, horizontalParams)

        vLine = FrameLayout(this).apply {
            setBackgroundColor(0x80FFFFFF.toInt())
        }
        val verticalParams = WindowManager.LayoutParams(
            initLineWidth,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        )
        verticalParams.gravity = Gravity.CENTER
        windowManager.addView(vLine, verticalParams)

        scope.launch {
            while (true) {
                var pkg = getForegroundApp()
                if (pkg == null)
                    continue

                // 1. if config doesn't exist, use default
                if (!settings.hasConfigOfPackage(pkg)) {
                    pkg = "default"
                }

                // 2. if current pkg is different from last pkg, re-subscribe
                if (pkg != currentPkg) {
                    currentPkg = pkg
                    subscribeToConfig(currentPkg)
                }

                delay(1000)
            }
        }
    }

    private fun subscribeToConfig(packageName: String) {
        Log.d("OverlayService", "Subscribing to config for package: $packageName")
        configJob?.cancel()
        configJob = scope.launch {
            settings.loadConfigFlowForApp(packageName).collect { conf ->
                applyConfig(conf.hOffset, conf.vOffset, conf.dotSize, conf.lineWidth)
            }
        }
    }

    private fun getForegroundApp(): String? {
        var foregroundApp: String? = null

        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()

        val usageEvents = usageStatsManager.queryEvents(time - 1000 * 3600, time)
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundApp = event.packageName
            }
        }

        return foregroundApp
    }

    private fun convertOffsetToPixels(offset: Float, axis: String): Int {
        val metrics = resources.displayMetrics
        val maxOffsetPx = if (axis == "horizontal") metrics.widthPixels / 2 else metrics.heightPixels / 2
        return (offset * maxOffsetPx).toInt()
    }

    private fun applyConfig(hOffset: Float? = null, vOffset: Float? = null, dotSize: Int? = null, lineWidth: Int? = null) {
        Log.d("OverlayService", "Applying config $hOffset $vOffset $dotSize $lineWidth")

        for (view in arrayOf(dot, hLine, vLine)) {
            if (hOffset != null)
                (view.layoutParams as WindowManager.LayoutParams).x = convertOffsetToPixels(hOffset, axis = "horizontal")
            if (vOffset != null)
                (view.layoutParams as WindowManager.LayoutParams).y = convertOffsetToPixels(vOffset, axis = "vertical")

            if (view == dot && dotSize != null) {
                (view.layoutParams as WindowManager.LayoutParams).width = dotSize
                (view.layoutParams as WindowManager.LayoutParams).height = dotSize
            }
            else if (view == hLine && lineWidth != null) {
                (view.layoutParams as WindowManager.LayoutParams).height = lineWidth
            }
            else if (view == vLine && lineWidth != null) {
                (view.layoutParams as WindowManager.LayoutParams).width = lineWidth
            }

            windowManager.updateViewLayout(view, view.layoutParams)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "overlay_channel"
        val channelName = "Overlay Notification Channel"

        val channel = NotificationChannel(
            channelId, channelName, NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val stopIntent = Intent(this, StopServiceReceiver::class.java)
        stopIntent.putExtra("service", 0)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Overlay Active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.cancel_24px,
                "Stop",
                stopPendingIntent
            )
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()

        isRunning.value = false

        scope.cancel()

        if (::dot.isInitialized) {
            windowManager.removeView(dot)
        }
        if (::vLine.isInitialized) {
            windowManager.removeView(vLine)
        }
        if (::hLine.isInitialized) {
            windowManager.removeView(hLine)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}