package dev.ujhhgtg.pandorasbox.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.receivers.StopServiceReceiver
import dev.ujhhgtg.pandorasbox.utils.DlnaController
import dev.ujhhgtg.pandorasbox.utils.PermissionManager
import dev.ujhhgtg.pandorasbox.utils.ServiceLocator
import dev.ujhhgtg.pandorasbox.utils.settings.PrefsRepository
import dev.ujhhgtg.pandorasbox.utils.SimpleFileServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jupnp.UpnpService
import org.jupnp.UpnpServiceImpl
import org.jupnp.android.AndroidRouter
import org.jupnp.android.AndroidUpnpServiceConfiguration
import org.jupnp.model.message.header.STAllHeader
import org.jupnp.model.meta.RemoteDevice
import org.jupnp.protocol.ProtocolFactory
import org.jupnp.registry.DefaultRegistryListener
import org.jupnp.registry.Registry
import org.jupnp.transport.Router


class DlnaServerService: Service() {
    private lateinit var settings: PrefsRepository
    companion object {
        @Volatile
        var isRunning: MutableState<Boolean> = mutableStateOf(false)
        const val TAG: String = "PB.DlnaServerService"
        const val ID: Int = 4
    }

    override fun onCreate() {
        super.onCreate()

        if (!PermissionManager.checkNotifications(this)) {
            Log.d(TAG, "required permissions are not granted")
            throw IllegalAccessException("required permissions are not granted")
        }

        isRunning.value = true
        startForegroundServiceWithNotification()
        settings = PrefsRepository(this)
        ServiceLocator.register(this)
        upnp = object : UpnpServiceImpl(AndroidUpnpServiceConfiguration()) {
            override fun createRouter(
                protocolFactory: ProtocolFactory?,
                registry: Registry?
            ): Router? {
                return AndroidRouter(
                    getConfiguration(), protocolFactory, this@DlnaServerService
                )
            }

            override fun shutdown() {
                (getRouter() as AndroidRouter).unregisterBroadcastReceiver()
                super.shutdown(true)
            }
        }
        upnp.startup()
        upnp.registry.addListener(registryListener)
        upnp.controlPoint.search(STAllHeader())
        Log.i(TAG, "started upnp service")
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "dlna_server_channel"
        val channelName = "DLNA Server Notification Channel"

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
            .setContentTitle("DLNA Server Active")
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var upnp: UpnpService

    private val _deviceListFlow = MutableStateFlow<List<RemoteDevice>>(emptyList())
    val deviceListFlow: StateFlow<List<RemoteDevice>> = _deviceListFlow

    private val _selectedDeviceFlow = MutableStateFlow<RemoteDevice?>(null)
    val selectedDeviceFlow: StateFlow<RemoteDevice?> = _selectedDeviceFlow

    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow

    private val _positionSecFlow = MutableStateFlow(0L)
    val positionSecFlow: StateFlow<Long> = _positionSecFlow

    private val _durationSecFlow = MutableStateFlow(0L)
    val durationSecFlow: StateFlow<Long> = _durationSecFlow

    private val _volumeFlow = MutableStateFlow(50)
    val volumeFlow: StateFlow<Int> = _volumeFlow

    private val _audioTracksFlow = MutableStateFlow<List<String>>(emptyList())
    val audioTracksFlow: StateFlow<List<String>> = _audioTracksFlow

    private val _subtitleTracksFlow = MutableStateFlow<List<String>>(emptyList())
    val subtitleTracksFlow: StateFlow<List<String>> = _subtitleTracksFlow

    private val _selectedAudioFlow = MutableStateFlow<String?>(null)
    val selectedAudioFlow: StateFlow<String?> = _selectedAudioFlow

    private val _selectedSubtitleFlow = MutableStateFlow<String?>(null)
    val selectedSubtitleFlow: StateFlow<String?> = _selectedSubtitleFlow

    private var controller: DlnaController? = null

    private val registryListener = object : DefaultRegistryListener() {
        override fun remoteDeviceDiscoveryStarted(registry: Registry?, device: RemoteDevice?) {
            Log.d(TAG, "remote device discovery started")
        }

        override fun remoteDeviceDiscoveryFailed(
            registry: Registry?,
            device: RemoteDevice?,
            e: Exception?
        ) {
            Log.e(TAG, "remote device discovery failed!", e)
        }

        override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
            Log.d(TAG, "remote device added: ${device.displayString}")
            _deviceListFlow.value = (_deviceListFlow.value + device).distinctBy { it.identity.udn }
        }

        override fun remoteDeviceRemoved(registry: Registry, device: RemoteDevice) {
            Log.d(TAG, "remote device removed: ${device.displayString}")
            _deviceListFlow.value = _deviceListFlow.value.filterNot { it.identity.udn == device.identity.udn }
        }
    }

    fun selectDevice(device: RemoteDevice) {
        _selectedDeviceFlow.value = device
        controller = DlnaController(upnp, device)
        startPolling()
    }

    fun serveAndPlayUri(cr: ContentResolver, uri: Uri, mime: String) {
        scope.launch {
            val server = SimpleFileServer(cr, uri, 8090)
            server.startSafe()
            val localUrl = server.getLocalUrl(this@DlnaServerService)
            controller?.setAVTransportURI(localUrl, meta = "")
            controller?.play()
        }
    }

    fun play() { controller?.play() }
    fun pause() { controller?.pause() }
    fun stop() { controller?.stop() }
    fun next() { controller?.next() }
    fun previous() { controller?.previous() }
    fun seekTo(targetSec: Long) { controller?.seekTo(targetSec) }
    fun setVolume(v: Int) { controller?.setVolume(v.coerceIn(0, 100)) }
    fun selectAudioTrack(id: String) { _selectedAudioFlow.value = id.ifEmpty { null }; controller?.selectAudioTrack(id) }
    fun selectSubtitle(id: String) { _selectedSubtitleFlow.value = id.ifEmpty { null }; controller?.selectSubtitle(id) }

    private fun startPolling() {
        scope.launch {
            while (isActive) {
                controller?.queryPosition()?.let { (pos, dur, playing) ->
                    _positionSecFlow.value = pos
                    _durationSecFlow.value = dur
                    _isPlayingFlow.value = playing
                }
                controller?.getVolume()?.let { _volumeFlow.value = it }
                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        isRunning.value = false
        upnp.registry.removeListener(registryListener)
        upnp.shutdown()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
