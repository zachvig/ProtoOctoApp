package de.crysxd.octoapp.octoprint.websocket

import com.google.gson.Gson
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

const val PING_TIMEOUT_MS = 3000L
const val CONNECTION_TIMEOUT_MS = 3000L
const val RECONNECT_DELAY_MS = 1000L
const val RECONNECT_TIMEOUT_MS = CONNECTION_TIMEOUT_MS + RECONNECT_DELAY_MS

class EventWebSocket(
    private val httpClient: OkHttpClient,
    private val hostname: String,
    private val port: Int,
    private val gson: Gson,
    private val logger: Logger
) {

    private var reconnectJob: Job? = null
    private var reportDisconnectedJob: Job? = null
    private var webSocket: WebSocket? = null
    private var isConnected = AtomicBoolean(false)
    private val eventHandlers: MutableList<Pair<CoroutineScope, suspend (Event) -> Unit>> = mutableListOf()
    private var lastCurrentMessage: Message.CurrentMessage? = null

    fun start() {
        if (isConnected.compareAndSet(false, true)) {
            val request = Request.Builder()
                .url("http://$hostname:$port/sockjs/websocket")
                .build()

            httpClient.newBuilder()
                .pingInterval(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .connectTimeout(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
                .newWebSocket(request, WebSocketListener())
        }
    }

    fun stop() {
        webSocket?.cancel()
        reconnectJob?.cancel()
        reportDisconnectedJob?.cancel()
        dispatchEvent(Event.Disconnected())
    }

    fun addEventHandler(scope: CoroutineScope, handler: suspend (Event) -> Unit) {
        eventHandlers.add(Pair(scope, handler))
    }

    fun removeEventHandler(handler: suspend (Event) -> Unit) {
        eventHandlers.removeAll {
            it.second == handler
        }
    }

    fun clearEventHandlers() {
        eventHandlers.clear()
    }

    private fun dispatchEvent(event: Event) {
        val e = CoroutineExceptionHandler { _, throwable -> throwable.printStackTrace() }
        eventHandlers.forEach {
            it.first.launch(e) {
                it.second(event)
            }
        }
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
            reportDisconnectedJob?.cancel()
            reportDisconnectedJob = null
            dispatchEvent(Event.Connected)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            try {
                val message = gson.fromJson(text, Message::class.java)
                if (message is Message.CurrentMessage) {
                    lastCurrentMessage = message
                }
                dispatchEvent(Event.MessageReceived(message))
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while parsing websocket message", e)
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            isConnected.set(false)
            dispatchEvent(Event.Disconnected())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            isConnected.set(false)

            logger.log(Level.WARNING, "Websocket encountered failure", t)

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