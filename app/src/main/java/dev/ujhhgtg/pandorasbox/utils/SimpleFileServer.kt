package dev.ujhhgtg.pandorasbox.utils


import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.net.Inet4Address
import java.net.NetworkInterface

class SimpleFileServer(private val cr: ContentResolver, private val uri: Uri, port: Int) : NanoHTTPD(port) {
    companion object {
        const val TAG: String = "PB.SimpleFileServer"
    }

    fun startSafe() { try { start(SOCKET_READ_TIMEOUT, false) } catch (_: Exception) {} }

    override fun serve(session: IHTTPSession?): Response {
        val input = cr.openInputStream(uri)
        val mime = cr.getType(uri) ?: "application/octet-stream"
        return newFixedLengthResponse(Response.Status.OK, mime, input, input?.available()?.toLong() ?: -1L)
    }

    fun getLocalUrl(ctx: Context): String {
        var ip = getLocalIpV4()
        if (ip == null) {
            Log.w(TAG, "cannot get local ipv4, falling back to localhost; casting would not work properly")
            ip = "127.0.0.1"
        }
        return "http://$ip:$listeningPort/"
    }

    private fun getLocalIpV4(): String? {
        val nis = NetworkInterface.getNetworkInterfaces()
        for (ni in nis) {
            if (!ni.isUp || ni.isLoopback) continue
            for (address in ni.inetAddresses) {
                if (address is Inet4Address && !address.isLoopbackAddress) return address.hostAddress
            }
        }
        return null
    }
}