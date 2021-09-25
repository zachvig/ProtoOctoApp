package de.crysxd.octoapp.octoprint.websocket

import de.crysxd.octoapp.octoprint.UPNP_ADDRESS_PREFIX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Dns
import okhttp3.HttpUrl
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger

class ContinuousOnlineCheck(
    private val url: HttpUrl,
    private val logger: Logger,
    private val onOnline: () -> Unit,
    private val connectionTimeoutMs: Int = 3_000,
    private val intervalMs: Long = 15_000L,
    private val localDns: Dns? = null,
) {
    private var checkJob: Job? = null

    fun start() {
        stop()
        checkJob = GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                delay(intervalMs)
                if (checkNow()) {
                    delay(intervalMs * 2)
                }
            }
        }
    }

    fun checkNow() = try {
        // Try to ping
        val url = url.toUrl()
        val newHost = if (url.host.endsWith(".local") || url.host.startsWith(UPNP_ADDRESS_PREFIX)) {
            localDns?.lookup(url.host)?.firstOrNull()?.hostAddress ?: InetAddress.getByName(url.host).hostName
        } else {
            url.host
        }
        InetAddress.getByName(newHost).isReachable(connectionTimeoutMs)
        val resolvedUrl = URL(url.protocol, newHost, url.port, url.file)

        // Try to connect
        val connection = resolvedUrl.openConnection() as HttpURLConnection
        connection.connectTimeout = connectionTimeoutMs
        connection.requestMethod = "HEAD"
        connection.connect()

        // Got any response code except 500? Online.
        if (connection.responseCode in 0..499) {
            logger.log(Level.INFO, "$url is ONLINE (${connection.responseCode})")
            onOnline()
            true
        } else {
            logger.log(Level.INFO, "$url is OFFLINE (${connection.responseCode})")
            false
        }
    } catch (e: Exception) {
        logger.log(Level.INFO, "$url is OFFLINE (${e::class.java.simpleName}: ${e.message})")
        // Well...too bad. Offline.
        false
    }

    private fun String.isLocalAddress() = startsWith("192.168.") ||
            startsWith("172.1") ||
            startsWith("10.")

    fun stop() {
        checkJob?.cancel()
    }
}