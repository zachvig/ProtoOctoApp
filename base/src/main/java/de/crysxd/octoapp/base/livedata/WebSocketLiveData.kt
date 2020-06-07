package de.crysxd.octoapp.base.livedata

import androidx.lifecycle.LiveData
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.websocket.EventWebSocket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.plus

class WebSocketLiveData(private val webSocket: EventWebSocket) : LiveData<Event>() {

    private var job: Job? = null
    private val handler = this::onEvent

    override fun onActive() {
        super.onActive()
        job = Job()
        webSocket.addEventHandler(GlobalScope + job!!, handler)
        webSocket.start()
    }

    override fun onInactive() {
        super.onInactive()
        job?.cancel()
        webSocket.stop()
        webSocket.removeEventHandler(handler)
    }

    private suspend fun onEvent(event: Event) {
        postValue(event)
    }
}