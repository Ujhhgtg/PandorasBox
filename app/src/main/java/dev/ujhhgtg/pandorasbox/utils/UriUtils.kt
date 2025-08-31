package dev.ujhhgtg.pandorasbox.utils

import android.util.Log
import androidx.core.net.toUri
import java.net.Inet6Address
import java.net.InetAddress
import java.util.regex.Pattern

private const val TAG: String = "PB.UriUtils"
object UriUtils {
    fun String?.withoutScheme(): String {
        if (this.isNullOrEmpty()) return ""
        return try {
            val uri = this.toUri()
            // does not remove uncommon schemes
            if (uri.scheme != "http" && uri.scheme != "https") {
                this
            }

            val host = uri.host ?: return this.removePrefix("${uri.scheme}://")
            if (uri.port != -1) "$host:${uri.port}${uri.path ?: ""}" else "$host${uri.path ?: ""}"
        } catch (_: Exception) {
            Log.w(TAG, "exception while removing scheme from uri $this")
            this
        }
    }

    fun String.isUri(): Boolean {
        val uri = try {
            this.toUri()
        } catch (_: Exception) {
            return false
        }

        // Special case: file:// URIs
        if (uri.scheme == "file") {
            return this.startsWith("file://")
        }

        // If no scheme, treat whole string as host (like "example.com")
        val host = uri.host ?: this.takeIf { !it.contains("://") } ?: return false

        return isValidHost(host)
    }

    private fun isValidHost(host: String): Boolean {
        // localhost
        if (host.equals("localhost", ignoreCase = true)) return true

        // IPv4
        if (isValidIPv4(host)) return true

        // IPv6 (bracketed or not)
        if (isValidIPv6(host)) return true

        // Domain like sub.example.com
        val domainRegex = Pattern.compile(
            "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*\\.?$"
        )
        if (domainRegex.matcher(host).matches()) {
            // Must have at least one dot or be localhost
            return host.contains(".")
        }

        return false
    }

    private fun isValidIPv4(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all {
            it.toIntOrNull()?.let { num -> num in 0..255 } != null
        }
    }

    private fun isValidIPv6(ip: String): Boolean {
        val clean = if (ip.startsWith("[") && ip.endsWith("]")) {
            ip.substring(1, ip.length - 1)
        } else ip

        return try {
            val addr = InetAddress.getByName(clean)
            addr is Inet6Address
        } catch (e: Exception) {
            false
        }
    }
}