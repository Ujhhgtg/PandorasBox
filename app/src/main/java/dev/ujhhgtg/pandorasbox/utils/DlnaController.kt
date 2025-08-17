package dev.ujhhgtg.pandorasbox.utils


import org.jupnp.UpnpService
import org.jupnp.controlpoint.ActionCallback
import org.jupnp.model.action.ActionInvocation
import org.jupnp.model.message.UpnpResponse
import org.jupnp.model.meta.RemoteDevice
import org.jupnp.model.meta.Service
import org.jupnp.model.types.UDAServiceType
import org.jupnp.model.types.UnsignedIntegerFourBytes
import org.jupnp.model.types.UnsignedIntegerTwoBytes
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DlnaController(private val upnp: UpnpService, device: RemoteDevice) {
    private val avTransport: Service<*, *>? = device.findService(UDAServiceType("AVTransport"))
    private val rendering: Service<*, *>? = device.findService(UDAServiceType("RenderingControl"))

    fun setAVTransportURI(uri: String, meta: String) = execute(avTransport, "SetAVTransportURI") {
        setInput("InstanceID", UnsignedIntegerFourBytes(0))
        setInput("CurrentURI", uri)
        setInput("CurrentURIMetaData", meta)
    }

    fun play() = execute(avTransport, "Play") {
        setInput("InstanceID", UnsignedIntegerFourBytes(0))
        setInput("Speed", "1")
    }

    fun pause() = execute(avTransport, "Pause") { setInput("InstanceID", UnsignedIntegerFourBytes(0)) }
    fun stop() = execute(avTransport, "Stop") { setInput("InstanceID", UnsignedIntegerFourBytes(0)) }
    fun next() = execute(avTransport, "Next") { setInput("InstanceID", UnsignedIntegerFourBytes(0)) }
    fun previous() = execute(avTransport, "Previous") { setInput("InstanceID", UnsignedIntegerFourBytes(0)) }

    fun seekTo(targetSec: Long) = execute(avTransport, "Seek") {
        setInput("InstanceID", UnsignedIntegerFourBytes(0))
        setInput("Unit", "REL_TIME")
        setInput("Target", secToHms(targetSec))
    }

    fun getVolume(): Int? {
        val svc = rendering ?: return null
        val act = ActionInvocation(svc.getAction("GetVolume"))
        act.setInput("InstanceID", UnsignedIntegerFourBytes(0))
        act.setInput("Channel", "Master")
        val latch = CountDownLatch(1)
        var out: Int? = null
        upnp.controlPoint.execute(object : ActionCallback(act) {
            override fun success(invocation: ActionInvocation<out Service<*, *>>?) {
                val v = act.getOutput("CurrentVolume").value as UnsignedIntegerTwoBytes
                out = v.value.toInt()
                latch.countDown()
            }
            override fun failure(invocation: ActionInvocation<out Service<*, *>>?, response: UpnpResponse?, defaultMsg: String?) { latch.countDown() }
        })
        latch.await(1200, TimeUnit.MILLISECONDS)
        return out
    }

    fun setVolume(v: Int) = execute(rendering, "SetVolume") {
        setInput("InstanceID", UnsignedIntegerFourBytes(0))
        setInput("Channel", "Master")
        setInput("DesiredVolume", UnsignedIntegerTwoBytes(v.toLong()))
    }

    fun queryPosition(): Triple<Long, Long, Boolean>? {
        val svc = avTransport ?: return null
        var state = "UNKNOWN"
        runBlockingAction(svc, "GetTransportInfo",
            fill = { inv ->
                inv.setInput("InstanceID", UnsignedIntegerFourBytes(0))
            },
            onSuccess = { act ->
                state = act.getOutput("CurrentTransportState").value as String
            }
        )

        var pos = 0L; var dur = 0L
        runBlockingAction(svc, "GetPositionInfo",
            fill = { inv ->
                inv.setInput("InstanceID", UnsignedIntegerFourBytes(0))
            },
            onSuccess = { act ->
                pos = hmsToSec(act.getOutput("RelTime").value as String)
                dur = hmsToSec(act.getOutput("TrackDuration").value as String)
            }
        )
        return Triple(pos, dur.coerceAtLeast(0), state == "PLAYING")
    }

    fun selectAudioTrack(id: String) {
        val names = listOf("SetCurrentAudioTrack", "X_SetAudioTrack")
        for (n in names) if (executeOptional(avTransport, n) { setInput("InstanceID", UnsignedIntegerFourBytes(0)); setInput("AudioTrack", id) }) return
    }

    fun selectSubtitle(id: String) {
        val names = listOf("SetSubtitle", "SetSubtitleTrack", "X_SetSubtitle")
        for (n in names) if (executeOptional(avTransport, n) { setInput("InstanceID", UnsignedIntegerFourBytes(0)); setInput("Subtitle", id) }) return
    }

    private fun executeOptional(svc: Service<*, *>?, action: String, fill: ActionInvocation<*>.() -> Unit): Boolean {
        val s = svc ?: return false
        val a = s.getAction(action) ?: return false
        val inv = ActionInvocation(a)
        fill(inv)
        val latch = CountDownLatch(1)
        var ok = false
        upnp.controlPoint.execute(object : ActionCallback(inv) {
            override fun success(invocation: ActionInvocation<out Service<*, *>>?) { ok = true; latch.countDown() }
            override fun failure(invocation: ActionInvocation<out Service<*, *>>?, response: UpnpResponse?, defaultMsg: String?) { latch.countDown() }
        })
        latch.await(1200, TimeUnit.MILLISECONDS)
        return ok
    }

    private fun execute(svc: Service<*, *>?, action: String, fill: ActionInvocation<*>.() -> Unit) {
        val s = svc ?: return
        val a = s.getAction(action) ?: return
        val inv = ActionInvocation(a)
        fill(inv)
        upnp.controlPoint.execute(object : ActionCallback(inv) {
            override fun success(invocation: ActionInvocation<out Service<*, *>>?) {}
            override fun failure(invocation: ActionInvocation<out Service<*, *>>?, response: UpnpResponse?, defaultMsg: String?) {}
        })
    }

    private fun runBlockingAction(svc: Service<*, *>, action: String, fill: (ActionInvocation<*>) -> Unit, timeoutMs: Long = 1200, onSuccess: (ActionInvocation<*>) -> Unit = {}): Result<Unit> {
        val act = svc.getAction(action) ?: return Result.failure(IllegalStateException("Action $action not found"))
        val inv = ActionInvocation(act)
        fill(inv)
        val latch = CountDownLatch(1)
        var ok = false
        upnp.controlPoint.execute(object : ActionCallback(inv) {
            override fun success(invocation: ActionInvocation<out Service<*, *>>?) { ok = true; latch.countDown() }
            override fun failure(invocation: ActionInvocation<out Service<*, *>>?, response: UpnpResponse?, defaultMsg: String?) { latch.countDown() }
        })
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        if (ok) onSuccess(inv)
        return if (ok) Result.success(Unit) else Result.failure(IllegalStateException("Action $action failed or timed out"))
    }

    private fun secToHms(sec: Long): String {
        val s = (sec % 60).toInt(); val m = ((sec / 60) % 60).toInt(); val h = (sec / 3600).toInt()
        return String.format(Locale.ROOT, "%02d:%02d:%02d", h, m, s)
    }
    private fun hmsToSec(hms: String?): Long {
        if (hms == null || hms.isEmpty() || hms == "NOT_IMPLEMENTED") return 0
        val p = hms.split(":"); if (p.size != 3) return 0
        return p[0].toLong() * 3600 + p[1].toLong() * 60 + p[2].toLong()
    }
}