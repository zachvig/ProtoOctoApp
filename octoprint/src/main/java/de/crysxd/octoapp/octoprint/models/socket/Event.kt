package de.crysxd.octoapp.octoprint.models.socket

sealed class Event {
    object Connected : Event()
    data class Disconnected(val exception: Throwable? = null) : Event()
    data class MessageReceived(val message: Message, private val isSelfGenerated: Boolean = false) : Event()
}