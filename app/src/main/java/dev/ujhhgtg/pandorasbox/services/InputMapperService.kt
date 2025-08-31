package dev.ujhhgtg.pandorasbox.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.receivers.StopServiceReceiver
import dev.ujhhgtg.pandorasbox.ui.composables.screens.InputMapperEditorScreen
import dev.ujhhgtg.pandorasbox.ui.theme.AppTheme
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import dev.ujhhgtg.pandorasbox.utils.ServiceLocator
import dev.ujhhgtg.pandorasbox.utils.settings.PrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

class InputMapperService : Service(), LifecycleOwner {
    private lateinit var settings: PrefsRepository
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private lateinit var lifecycleRegistry: LifecycleRegistry

    companion object {
        @Volatile
        var isRunning: MutableState<Boolean> = mutableStateOf(false)
        const val TAG: String = "PB.InputMapperService"
        const val ID: Int = 3
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PermissionManager.checkNotifications(this) ||
            !PermissionManager.checkOverlay(this) ||
            !PermissionManager.checkShizuku()) {
            Log.d(TAG, "required permissions are not granted")
            throw IllegalAccessException("required permissions are not granted")
        }

        val serverUri = intent?.getStringExtra("uri") ?: return START_NOT_STICKY
        isRunning.value = true
        startForegroundServiceWithNotification()
        settings = PrefsRepository(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        ServiceLocator.register(this)

        composeView = ComposeView(this).apply {
//            ViewTreeLifecycleOwner(this, this@InputMapperService)
            setContent {
                AppTheme {
                    InputMapperEditorScreen(
                        onClose = {
                            stopSelf()
                        }
                    )
                }
            }
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            ,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager.addView(composeView, layoutParams)

        webSocketClient = object : WebSocketClient(URI(serverUri)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d(TAG, "websocket opened")
            }

            override fun onMessage(message: String?) {
                message?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val json = JSONObject(it)
                            when (json.getString("action")) {
                                "tap" -> {
                                    val x = json.getInt("x")
                                    val y = json.getInt("y")
                                    simulateTap(x, y)
                                }
                                "swipe" -> {
                                    val x1 = json.getInt("x1")
                                    val y1 = json.getInt("y1")
                                    val x2 = json.getInt("x2")
                                    val y2 = json.getInt("y2")
                                    val duration = json.optInt("duration", 100)
                                    simulateSwipe(x1, y1, x2, y2, duration)
                                }
                                "hold" -> {
                                    val x = json.getInt("x")
                                    val y = json.getInt("y")
                                    val duration = json.getInt("duration")
                                    simulateSwipe(x, y, x, y, duration)
                                }
                                "press" -> {
                                    val x = json.getInt("x")
                                    val y = json.getInt("y")
                                    simulatePress(x, y)
                                }
                                "release" -> {
                                    val x = json.getIntOrElse("x", null)
                                    val y = json.getIntOrElse("y", null)
                                    simulateRelease(x, y)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "websocket received invalid message: $message", e)
                        }
                    }
                }
            }

            private fun JSONObject.getIntOrElse(name: String, elseValue: Int?): Int? {
                return try {
                    this.getInt(name)
                } catch (_: JSONException) {
                    elseValue
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG, "websocket closed: $reason")
            }

            override fun onError(ex: Exception?) {
                Log.e(TAG, "websocket error", ex)
            }
        }
        webSocketClient.connect()

        return START_STICKY
    }

    private fun simulateTap(x: Int, y: Int) {
        val command = "input tap $x $y"
        executeRootCommand(command)
    }

    private var lastPressX: Int? = null
    private var lastPressY: Int? = null

    private fun simulatePress(x: Int, y: Int) {
        lastPressX = x
        lastPressY = y
        val command = "input motionevent DOWN $x $y"
        executeRootCommand(command)
    }

    private fun simulateRelease(x: Int? = null, y: Int? = null) {
        var actualX = x
        var actualY = y
        if (actualX == null && lastPressX != null) {
            actualX = lastPressX
        }
        if (actualY == null && lastPressY != null) {
            actualY = lastPressY
        }

        if (actualX == null || actualY == null) {
            Log.w(TAG, "x or y is null when simulating release, ignoring")
            return
        }

        lastPressX = null
        lastPressY = null

        val command = "input motionevent UP $actualX $actualY"
        executeRootCommand(command)
    }

    private fun simulateSwipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int? = null) {
        val command = if (duration != null)
            "input swipe $x1 $y1 $x2 $y2 $duration"
        else
            "input swipe $x1 $y1 $x2 $y2"
        executeRootCommand(command)
    }

    private fun executeRootCommand(command: String) {
        try {
            /*val process =*/ Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            // val returnCode = process.waitFor()
            Log.d(TAG, "executed: $command")
        } catch (e: Exception) {
            Log.e(TAG, "failed to execute: $command", e)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "input_mapper_channel"
        val channelName = "Input Mapper Notification Channel"

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
            .setContentTitle("Input Mapper Active")
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

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        if (::composeView.isInitialized) {
            windowManager.removeView(composeView)
        }

        isRunning.value = false
        webSocketClient.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}