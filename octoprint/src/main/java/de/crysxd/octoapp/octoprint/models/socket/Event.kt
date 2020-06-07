package de.crysxd.octoapp.octoprint.models.socket

import java.lang.Exception

sealed class Event {
    object Connected : Event()
    data class Disconnected(val exception: Throwable? = null) : Event()
    data class MessageReceived(val message: Message) : Event()
}