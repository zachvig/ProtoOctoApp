package de.crysxd.octoapp.octoprint.websocket

import com.google.gson.Gson
import de.crysxd.octoapp.octoprint.api.LoginApi
import de.crysxd.octoapp.octoprint.exceptions.*
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger

const val RECONNECT_DELAY_MS = 1000L

@Suppress("EXPERIMENTAL_API_USAGE")
class EventWebSocket(
    private val httpClient: OkHttpClient,
    private val webUrl: String,
    private val loginApi: LoginApi,
    private val gson: Gson,
    private val logger: Logger,
    private val pingPongTimeoutMs: Long,
    private val connectionTimeoutMs: Long,
) {

    private val reconnectTimeout = connectionTimeoutMs + RECONNECT_DELAY_MS
    private val stalledTimeout = reconnectTimeout * 5

    private var reconnectJob: Job? = null
    private var reportDisconnectedJob: Job? = null
    private var webSocket: WebSocket? = null
    private var isConnected = AtomicBoolean(false)
    private var isOpen = false
    private var lastCurrentMessage: Message.CurrentMessage? = null
    private val channel = BroadcastChannel<Event>(15)
    private val subscriberCount = AtomicInteger(0)
    private val webSocketUrl = URI.create(webUrl).resolve("sockjs/websocket").toURL()

    fun start() {
        if (subscriberCount.get() > 0 && isConnected.compareAndSet(false, true)) {
            val request = Request.Builder()
                .url(webSocketUrl)
                .build()

            httpClient.newBuilder()
                .pingInterval(pingPongTimeoutMs, TimeUnit.MILLISECONDS)
                .connectTimeout(connectionTimeoutMs, TimeUnit.MILLISECONDS)
                .build()
                .newWebSocket(request, WebSocketListener())

            logger.log(Level.INFO, "Opening web socket")
        }
    }

    fun stop() {
        if (subscriberCount.get() == 0) {
            webSocket?.close(1000, "User exited app")
            webSocket?.cancel()
            reconnectJob?.cancel()
            reportDisconnectedJob?.cancel()
            dispatchEvent(Event.Disconnected())
            logger.log(Level.INFO, "Closing web socket")
            handleClosure()
        } else {
            logger.log(Level.INFO, "${subscriberCount.get()} subscribers still active, leaving socket open")
        }
    }

    fun passiveEventFlow(): Flow<Event> = channel.asFlow()

    fun eventFlow(tag: String): Flow<Event> {
        return channel.asFlow()
            .onStart {
                logger.log(Level.INFO, "onStart for Flow (tag=$tag, webSocket=${this@EventWebSocket})")
                subscriberCount.incrementAndGet()
                start()
            }
            .onCompletion {
                logger.log(Level.INFO, "onCompletion for Flow (tag=$tag, webSocket=${this@EventWebSocket})")
                subscriberCount.decrementAndGet()
                stop()
            }
    }

    private fun dispatchEvent(event: Event) {
        channel.offer(event)
    }

    private fun handleClosure() {
        isConnected.set(false)
        logger.log(Level.INFO, "Web socket closed")
        dispatchEvent(Event.Disconnected())
    }

    internal fun postMessage(message: Message) {
        dispatchEvent(Event.MessageReceived(message, true))
    }

    internal fun postCurrentMessageInterpolation(modifier: (Message.CurrentMessage) -> Message.CurrentMessage?) {
        lastCurrentMessage?.let {
            modifier(it)?.let(this::postMessage)
        }
    }

    inner class WebSocketListener : okhttp3.WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            isOpen = true

            // Handle open event
            logger.log(Level.INFO, "Web socket open")
            reportDisconnectedAfterDelay(WebSocketDysfunctionalException())
            dispatchEvent(Event.Connected)

            // In order to receive any messages on OctoPrint instances with authentication set up,
            // we need to perform a login and sen the "auth" message
            val login = runBlocking { loginApi.passiveLogin() }
            logger.log(Level.INFO, "Sending auth message for user \"${login.name}\"")
            webSocket.send("{\"auth\": \"${login.name}:${login.session}\"}")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            logger.log(Level.FINEST, "Message received: ${text.substring(0, 128.coerceAtMost(text.length))} ")

            // Report disconnected after a delay. The delay is reset the next time we receive a message,
            // so the disconnect is propagated if we do not receive a message after a set delay
            // This is only the case if we still receive pings but no messages (otherwise connecting fails)
            reportDisconnectedAfterDelay(WebSocketStalledException(), stalledTimeout)

            // OkHttp sometimes leaks connections.
            // If we are no longer supposed to be cIncreonnected, we crash the socket
            if (!isConnected.get()) {
                throw WebSocketZombieException()
            }

            try {
                val message = gson.fromJson(text, Message::class.java)

                if (message is Message.CurrentMessage) {
                    lastCurrentMessage = message
                }

                if (message is Message.ReAuthRequired) {
                    logger.log(Level.WARNING, "Web socket needs to authenticate again")
                    stop()
                    reconnect()
                } else {
                    dispatchEvent(Event.MessageReceived(message))
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while parsing webs socket message", e)
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            handleClosure()
            isOpen = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            when {
                t is WebSocketZombieException -> {
                    logger.log(Level.WARNING, "Web socket was forcefully closed")
                }
                t is OctoPrintApiException && t.responseCode >= 400 -> {
                    reconnect(WebSocketUpgradeFailedException(t.responseCode, webUrl), true)
                }
                else -> {
                    reconnect(t)
                }
            }

            isOpen = false
        }

        private fun reconnect(t: Throwable? = null, reportImmediately: Boolean = false) {
            isConnected.set(false)

            reconnectJob = GlobalScope.launch {
                delay(RECONNECT_DELAY_MS)
                start()
            }

            reportDisconnectedAfterDelay(
                throwable = t,
                delay = if (!isOpen || reportImmediately) 0 else reconnectTimeout
            )
        }

        private fun reportDisconnectedAfterDelay(throwable: Throwable?, delay: Long = reconnectTimeout) {
            reportDisconnectedJob?.cancel()
            reportDisconnectedJob = GlobalScope.launch {
                delay(delay)
                logger.log(Level.SEVERE, "Reporting disconnect", throwable)
                dispatchEvent(Event.Disconnected(throwable))
            }
        }
    }
}