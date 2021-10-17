package de.crysxd.octoapp.octoprint.websocket

import com.google.gson.Gson
import de.crysxd.octoapp.octoprint.api.LoginApi
import de.crysxd.octoapp.octoprint.exceptions.*
import de.crysxd.octoapp.octoprint.models.ConnectionType
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.resolvePath
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern

const val RECONNECT_DELAY_MS = 1000L

class EventWebSocket(
    private val httpClient: OkHttpClient,
    private val webUrl: HttpUrl,
    private val getCurrentConnectionType: () -> ConnectionType,
    private val loginApi: LoginApi,
    private val gson: Gson,
    private val logger: Logger,
    private val onStart: () -> Unit,
    private val onStop: () -> Unit,
    private val pingPongTimeoutMs: Long,
    private val connectionTimeoutMs: Long,
) {


    companion object {
        var instanceCounter = 0
    }

    private val webSocketId = "WS/${instanceCounter++}"
    private var listenerCounter = 0

    private val reconnectTimeout = connectionTimeoutMs + RECONNECT_DELAY_MS

    private var reportDisconnectedJob: Job? = null
    private var webSocket: WebSocket? = null
    private var webSocketListener: EventWebSocket.WebSocketListener? = null
    private var isConnected = AtomicBoolean(false)
    private var lastCurrentMessage: Message.CurrentMessage? = null
    private val eventFlow = MutableSharedFlow<Event>(15)
    private val subscriberCount = AtomicInteger(0)
    private val webSocketUrl = webUrl.resolvePath("sockjs/websocket")
    private var job = SupervisorJob()
    private val coroutineScope
        get() = CoroutineScope(job + Dispatchers.Main.immediate) + CoroutineExceptionHandler { _, throwable ->
            logger.log(Level.SEVERE, "NON-CONTAINED exception in coroutineScope", throwable)
        }

    private val logMaskPattern = Pattern.compile("\\[(.*?)]")


    fun start() {
        if (subscriberCount.get() > 0 && isConnected.compareAndSet(false, true)) {
            job.cancel()
            job = SupervisorJob()

            val request = Request.Builder()
                .url(webSocketUrl)
                .build()

            webSocketListener?.dispose()
            webSocketListener = WebSocketListener().also {
                webSocket = httpClient.newBuilder()
                    .pingInterval(pingPongTimeoutMs, TimeUnit.MILLISECONDS)
                    .connectTimeout(connectionTimeoutMs, TimeUnit.MILLISECONDS)
                    .build()
                    .newWebSocket(request, it)
            }

            logger.log(Level.INFO, "[$webSocketId] Opening web socket")
            onStart()
        }
    }

    fun stop() = if (subscriberCount.get() == 0) {
        doStop()
        dispatchEvent(Event.Disconnected())
    } else {
        logger.log(Level.INFO, "${subscriberCount.get()} subscribers still active, leaving socket open")
    }

    private fun doStop() {
        webSocketListener?.dispose()
        webSocket?.close(1000, "User exited app")
        webSocket?.cancel()
        job.cancel()
        reportDisconnectedJob?.cancel()
        logger.log(Level.INFO, "[$webSocketId] Closing web socket")
        handleClosure()
        onStop()
    }

    fun passiveEventFlow(): Flow<Event> = eventFlow.asSharedFlow()

    fun eventFlow(tag: String): Flow<Event> {
        return eventFlow.onStart {
            logger.log(Level.INFO, "[$webSocketId] onStart for Flow (tag=$tag, webSocket=${this@EventWebSocket})")
            subscriberCount.incrementAndGet()
            start()
        }.onCompletion {
            logger.log(Level.INFO, "[$webSocketId] onCompletion for Flow (tag=$tag, webSocket=${this@EventWebSocket})")
            subscriberCount.decrementAndGet()
            stop()
        }
    }

    private fun dispatchEvent(event: Event) {
        eventFlow.tryEmit(event)
    }

    private fun handleClosure() {
        isConnected.set(false)
        logger.log(Level.INFO, "[$webSocketId] Web socket closed")
    }

    internal fun postMessage(message: Message) {
        dispatchEvent(Event.MessageReceived(message, true))
    }

    internal fun postCurrentMessageInterpolation(modifier: (Message.CurrentMessage) -> Message.CurrentMessage?) {
        lastCurrentMessage?.let {
            modifier(it)?.let(this::postMessage)
        }
    }

    internal fun reconnect() {
        doStop()
        start()
    }

    inner class WebSocketListener : okhttp3.WebSocketListener() {
        var isOpen = false
        val listenerId = "$webSocketId/${listenerCounter++}"
        private var currentMessageCounter = 0
        private var firstMessage = true

        fun dispose() {
            logger.log(Level.INFO, "[$listenerId] Web socket disposed")
            isOpen = false
        }

        private fun shouldLogCurrentMessage() = currentMessageCounter++ % 5 == 0

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            isOpen = true
            firstMessage = true

            // Handle open event
            logger.log(Level.INFO, "[$listenerId] Web socket open")


            // In order to receive any messages on OctoPrint instances with authentication set up,
            // we need to perform a login and sen the "auth" message
            val login = runBlocking { loginApi.passiveLogin() }
            logger.log(Level.INFO, "Sending auth message for user \"${login.name}\"")
            webSocket.send("{\"auth\": \"${login.name}:${login.session}\"}")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)

            // OkHttp sometimes leaks connections.
            // If we are no longer supposed to be connected, we crash the socket
            if (!isOpen) {
                throw WebSocketZombieException()
            }

            // We only send the connected message after we received the first message. This is important because
            // OctoEverywhere connects the websocket even if OctoPrint is down. When we receive the first message
            // we know that we are connected.
            if (firstMessage) {
                firstMessage = false
                logger.log(Level.INFO, "[$listenerId] Web socket connected")
                dispatchEvent(Event.Connected(getCurrentConnectionType()))
            }

            logger.log(Level.FINEST, "[$listenerId] Message received: ${text.substring(0, 32.coerceAtMost(text.length))} ")
            reportDisconnectedJob?.cancel()

            try {
                when (val message = gson.fromJson(text, Message::class.java)) {
                    is Message.CurrentMessage -> {
                        lastCurrentMessage = message
                        dispatchEvent(Event.MessageReceived(message))
                        if (shouldLogCurrentMessage()) {
                            logger.log(Level.FINE, "[$listenerId] Current message received: ${logMaskPattern.matcher(text).replaceAll("[...]")} ")
                        }
                    }

                    is Message.ReAuthRequired -> {
                        logger.log(Level.WARNING, "[$listenerId] Web socket needs to authenticate again")
                        stop()
                        reconnect()
                    }

                    else -> dispatchEvent(Event.MessageReceived(message))
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "[$listenerId] Error while parsing webs socket message: $text", e)
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
                !isConnected.get() -> Unit
                t is WebSocketZombieException -> {
                    logger.log(Level.WARNING, "[$listenerId] Web socket was forcefully closed")
                }
                t is OctoPrintApiException && t.responseCode in 400..599 -> {
                    reconnect(WebSocketUpgradeFailedException(t.responseCode, webSocketUrl = webSocketUrl, webUrl = webUrl), true)
                }
                else -> {
                    reconnect(t)
                }
            }

            isOpen = false
        }

        private fun reconnect(t: Throwable? = null, reportImmediately: Boolean = false, reconnectDelay: Long = RECONNECT_DELAY_MS) {
            if (isOpen) {
                isConnected.set(false)

                coroutineScope.launch {
                    delay(reconnectDelay)
                    start()
                }

                reportDisconnectedAfterDelay(
                    throwable = t,
                    delay = if (!isOpen || reportImmediately) 0 else reconnectTimeout
                )
            }
        }

        private fun reportDisconnectedAfterDelay(throwable: Throwable?, delay: Long = reconnectTimeout) {
            reportDisconnectedJob?.cancel()
            reportDisconnectedJob = coroutineScope.launch {
                delay(delay)
                logger.log(Level.SEVERE, "Reporting disconnect", throwable)
                dispatchEvent(Event.Disconnected(throwable))
            }
        }
    }
}