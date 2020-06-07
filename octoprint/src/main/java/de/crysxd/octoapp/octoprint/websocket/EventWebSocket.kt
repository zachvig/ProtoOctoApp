package de.crysxd.octoapp.octoprint.websocket

import com.google.gson.Gson
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket

class EventWebSocket(
    private val httpClient: OkHttpClient,
    private val hostname: String,
    private val port: Int,
    private val eventHandler: (Event) -> Unit,
    private val gson: Gson
) {

    private var webSocket: WebSocket? = null

    fun start() {
        stop()

        val request = Request.Builder()
            .url("http://$hostname:$port/sockjs/websocket")
            .build()

        httpClient.newWebSocket(request, WebSocketListener())
    }

    fun stop() {
        webSocket?.cancel()
    }

    inner class WebSocketListener : okhttp3.WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            eventHandler(Event.Connected)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            eventHandler(Event.MessageReceived(gson.fromJson(text, Message::class.java)))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            eventHandler(Event.Disconnected())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            eventHandler(Event.Disconnected(t))
        }
    }
}