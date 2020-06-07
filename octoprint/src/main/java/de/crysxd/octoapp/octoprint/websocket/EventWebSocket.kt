package de.crysxd.octoapp.octoprint.websocket

import com.google.gson.Gson
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class EventWebSocket(
    private val httpClient: OkHttpClient,
    private val hostname: String,
    private val port: Int,
    private val gson: Gson
) {

    private var webSocket: WebSocket? = null
    private var isConnected = AtomicBoolean()
    private val eventHandlers: MutableList<Pair<CoroutineScope, suspend (Event) -> Unit>> = mutableListOf()

    fun start() {
        if (isConnected.getAndSet(true)) {

            val request = Request.Builder()
                .url("http://$hostname:$port/sockjs/websocket")
                .build()

            httpClient.newWebSocket(request, WebSocketListener())
        }
    }

    fun stop() {
        webSocket?.cancel()
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

    inner class WebSocketListener : okhttp3.WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            dispatchEvent(Event.Connected)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            dispatchEvent(Event.MessageReceived(gson.fromJson(text, Message::class.java)))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            isConnected.set(false)
            dispatchEvent(Event.Disconnected())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            isConnected.set(false)
            dispatchEvent(Event.Disconnected(t))
        }
    }
}