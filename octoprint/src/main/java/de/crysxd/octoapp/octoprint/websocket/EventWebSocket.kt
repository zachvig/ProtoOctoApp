package de.crysxd.octoapp.octoprint.websocket

import com.google.gson.Gson
import de.crysxd.octoapp.octoprint.api.LoginApi
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.asFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

const val PING_TIMEOUT_MS = 3000L
const val CONNECTION_TIMEOUT_MS = 3000L
const val RECONNECT_DELAY_MS = 1000L
const val RECONNECT_TIMEOUT_MS = CONNECTION_TIMEOUT_MS + RECONNECT_DELAY_MS

@Suppress("EXPERIMENTAL_API_USAGE")
class EventWebSocket(
    private val httpClient: OkHttpClient,
    private val webUrl: String,
    private val loginApi: LoginApi,
    private val gson: Gson,
    private val logger: Logger
) {

    private var reconnectJob: Job? = null
    private var reportDisconnectedJob: Job? = null
    private var webSocket: WebSocket? = null
    private var isConnected = AtomicBoolean(false)
    private var lastCurrentMessage: Message.CurrentMessage? = null
    private val channel = BroadcastChannel<Event>(15)

    fun start() {
        if (isConnected.compareAndSet(false, true)) {

            val request = Request.Builder()
                .url(
                    URI.create("$webUrl/")
                        .resolve(".") // Remove // at the end
                        .resolve("sockjs/websocket") // Add api/ to path
                        .toURL()
                )
                .build()

            httpClient.newBuilder()
                .pingInterval(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
                .newWebSocket(request, WebSocketListener())

            logger.log(Level.INFO, "Opening websocket")
        }
    }

    fun stop() {
        webSocket?.cancel()
        reconnectJob?.cancel()
        reportDisconnectedJob?.cancel()
        dispatchEvent(Event.Disconnected())
        logger.log(Level.INFO, "Closing websocket")
        handleClosure()
    }

    fun eventFlow() = channel.asFlow()

    private fun dispatchEvent(event: Event) {
        channel.offer(event)
    }

    private fun handleClosure() {
        isConnected.set(false)
        logger.log(Level.INFO, "Websocket closed")
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

            // Handle open event
            logger.log(Level.INFO, "Websocket open")
            reportDisconnectedJob?.cancel()
            reportDisconnectedJob = null
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

            try {
                val message = gson.fromJson(text, Message::class.java)

                if (message is Message.CurrentMessage) {
                    lastCurrentMessage = message
                }

                if (message is Message.ReAuthRequired) {
                    logger.log(Level.WARNING, "Websocket needs to authenticate again")
                    stop()
                    reconnect()
                } else {
                    dispatchEvent(Event.MessageReceived(message))
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while parsing websocket message", e)
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            handleClosure()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            logger.log(Level.WARNING, "Websocket encountered failure", t)
            reconnect(t)
        }

        private fun reconnect(t: Throwable? = null) {
            isConnected.set(false)

            reconnectJob = GlobalScope.launch {
                delay(RECONNECT_DELAY_MS)
                start()
            }

            // If not yet done, schedule to dispatch the disconnected event
            // This will be cancelled once connected
            if (reportDisconnectedJob == null) {
                reportDisconnectedJob = GlobalScope.launch {
                    delay(RECONNECT_TIMEOUT_MS)
                    dispatchEvent(Event.Disconnected(t))
                }
            }
        }
    }
}