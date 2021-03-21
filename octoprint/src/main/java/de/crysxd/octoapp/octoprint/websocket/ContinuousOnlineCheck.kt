package de.crysxd.octoapp.octoprint.websocket

import de.crysxd.octoapp.octoprint.UrlString
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger

class ContinuousOnlineCheck(
    private val url: UrlString,
    private val logger: Logger,
    private val onOnline: () -> Unit,
    private val connectionTimeoutMs: Int = 3_000,
    private val intervalMs: Long = 60_000L
) {
    private var checkJob: Job? = null

    fun start() {
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
        val url = URL(url)
        InetAddress.getByName(url.host).isReachable(connectionTimeoutMs)

        // Try to connect
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = connectionTimeoutMs
        connection.requestMethod = "HEAD"
        connection.connect()

        // Got any response code except 500? Online.
        if (connection.responseCode in 0..499) {
            logger.log(Level.INFO, "$url is ONLINE (${connection.responseCode})")
            onOnline()
            true
        } else {
            false
        }
    } catch (e: Exception) {
        logger.log(Level.INFO, "$url is OFFLINE")
        // Well...too bad. Offline.
        false
    }

    fun stop() {
        checkJob?.cancel()
    }
}