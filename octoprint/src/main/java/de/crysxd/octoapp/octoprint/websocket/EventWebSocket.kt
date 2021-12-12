package de.crysxd.octoapp.octoprint.websocket

import com.google.gson.Gson
import de.crysxd.octoapp.octoprint.api.LoginApi
import de.crysxd.octoapp.octoprint.exceptions.*
import de.crysxd.octoapp.octoprint.models.ConnectionType
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.octoprint.resolvePath
import de.crysxd.octoapp.octoprint.websocket.EventFlowConfiguration.Companion.ALL_LOGS
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
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.withLock

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
    private val webSocketUrl = webUrl.resolvePath("sockjs/websocket")
    private var webSocket: WebSocket? = null
    private var webSocketListener: EventWebSocket.WebSocketListener? = null
    private var isConnected = AtomicBoolean(false)
    private var reconnectCounter = 0
    private var eventFilters = mutableListOf<EventFlowConfiguration>()

    private var lastCurrentMessage: Message.CurrentMessage? = null
    private val eventFlow = MutableSharedFlow<Event>(15)
    private val subscriberCount = AtomicInteger(0)

    private var job = SupervisorJob()
    private val coroutineScope
        get() = CoroutineScope(job + Dispatchers.Main.immediate) + CoroutineExceptionHandler { _, throwable ->
            logger.log(Level.SEVERE, "NON-CONTAINED exception in coroutineScope", throwable)
        }

    fun start() {
        if (subscriberCount.get() > 0) {
            if (isConnected.compareAndSet(false, true)) {
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

            eventFilters.takeIf { it.isNotEmpty() }?.let { filters ->
                val config = EventWebSocketConfiguration(
                    throttle = EventWebSocketConfiguration.Throttle(filters.minOf { it.throttle }),
                    subscription = EventWebSocketConfiguration.Subscribe(
                        Subscription(
                            state = Subscription.State(
                                logs = filters.map { it.requestTerminalLogs }.flatten().distinct().let {
                                    when {
                                        it.isEmpty() -> false
                                        it.contains(ALL_LOGS) -> true
                                        else -> "(${it.joinToString("|")})"
                                    }
                                }
                            )
                        )
                    )
                )
                webSocketListener?.configure(config)
            }
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
        logger.log(Level.INFO, "[$webSocketId] Closing web socket")
        handleClosure()
        onStop()
    }

    fun passiveEventFlow(): Flow<Event> = eventFlow.asSharedFlow()

    fun eventFlow(tag: String, filter: EventFlowConfiguration = EventFlowConfiguration()) = eventFlow.onStart {
        logger.log(Level.INFO, "[$webSocketId] onStart for Flow (tag=$tag, webSocket=${this@EventWebSocket}, filters=$filter)")
        subscriberCount.incrementAndGet()
        eventFilters.add(filter)
        start()
    }.onCompletion {
        logger.log(Level.INFO, "[$webSocketId] onCompletion for Flow (tag=$tag, webSocket=${this@EventWebSocket})")
        subscriberCount.decrementAndGet()
        eventFilters.remove(filter)
        start()
        stop()
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

    private inner class WebSocketListener : okhttp3.WebSocketListener() {
        var isOpen = true
        val listenerId = "$webSocketId/${listenerCounter++}"
        private var currentMessageCounter = 0
        private var firstMessage = true
        private var pendingConfiguration: EventWebSocketConfiguration? = null
        private var webSocket: WebSocket? = null
        private var lastConfig: EventWebSocketConfiguration? = null
        private val configLock = ReentrantLock()

        init {
            logger.log(Level.INFO, "[$listenerId] Web socket created")
        }

        fun dispose() {
            logger.log(Level.INFO, "[$listenerId] Web socket disposed")
            isOpen = false
        }

        fun configure(config: EventWebSocketConfiguration) {
            webSocket.sendConfiguration(config)
        }

        private fun shouldLogCurrentMessage(text: String) = !text.startsWith("{\"history") && currentMessageCounter++ % 20 == 0

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            isOpen = true
            firstMessage = true
            this.webSocket = webSocket
            configLock.withLock {
                pendingConfiguration?.let { webSocket.sendConfiguration(it) }
            }

            // Handle open event
            logger.log(Level.INFO, "[$listenerId] Web socket open")

            // In order to receive any messages on OctoPrint instances with authentication set up,
            // we need to perform a login and sen the "auth" message
            val login = runBlocking { loginApi.passiveLogin() }
            logger.log(Level.INFO, "[$listenerId] Sending auth message for user \"${login.name}\"")
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
                reconnectCounter = 0
                logger.log(Level.INFO, "[$listenerId] Web socket connected")
                dispatchEvent(Event.Connected(getCurrentConnectionType()))
            }

            logger.log(Level.FINEST, "[$listenerId] Message received: ${text.substring(0, 32.coerceAtMost(text.length))} ")

            try {
                when (val message = gson.fromJson(text, Message::class.java)) {
                    is Message.CurrentMessage -> {
                        lastCurrentMessage = message
                        dispatchEvent(Event.MessageReceived(message))
                        if (shouldLogCurrentMessage(text)) {
                            text.chunked(128).forEach {
                                logger.log(Level.FINE, "[$listenerId] Current message ${currentMessageCounter - 1} received: $it")
                            }
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
            this.webSocket = null
            handleClosure()
            isOpen = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            logger.log(Level.WARNING, "[$listenerId] Websocket failure: ${t::class.java.simpleName}: ${t.message}")

            when {
                !isConnected.get() -> Unit
                t is WebSocketZombieException -> {
                    logger.log(Level.WARNING, "[$listenerId] Web socket was forcefully closed")
                }
                t is OctoPrintApiException && t.responseCode in 400..599 -> {
                    reconnect(WebSocketUpgradeFailedException(t.responseCode, webSocketUrl = webSocketUrl, webUrl = webUrl), reportImmediately = true)
                }
                else -> {
                    reconnect(t)
                }
            }

            isOpen = false
        }

        private fun reconnect(t: Throwable? = null, reportImmediately: Boolean = false) {
            if (isOpen) {
                isConnected.set(false)
                reconnectCounter++
                logger.log(Level.INFO, "[$listenerId] Reconnecting... ($reconnectCounter)")

                coroutineScope.launch {
                    delay(RECONNECT_DELAY_MS)
                    start()
                }

                if (reconnectCounter > 1 || reportImmediately) {
                    logger.log(Level.SEVERE, "[$listenerId] Reporting disconnect", t)
                    dispatchEvent(Event.Disconnected(t))
                }
            }
        }

        private fun WebSocket?.sendConfiguration(config: EventWebSocketConfiguration) = configLock.withLock {
            when {
                config == lastConfig -> Unit

                this == null -> {
                    logger.log(Level.INFO, "[$listenerId] Not ready to send config, setting as pending")
                    pendingConfiguration = config
                }

                else -> {
                    val subscription = gson.toJson(config.subscription)
                    val throttle = gson.toJson(config.throttle)
                    logger.log(Level.INFO, "[$listenerId] Sending configuration: $subscription")
                    logger.log(Level.INFO, "[$listenerId] Sending configuration: $throttle")
                    if (send(subscription) && send(throttle)) {
                        logger.log(Level.INFO, "[$listenerId] Send success")
                        pendingConfiguration = null
                        lastConfig = config
                    } else {
                        logger.log(Level.INFO, "[$listenerId] Send failure")
                        pendingConfiguration = config
                    }
                }
            }
        }
    }
}