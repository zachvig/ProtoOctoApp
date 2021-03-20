package de.crysxd.octoapp.octoprint.models.socket

import de.crysxd.octoapp.octoprint.models.ConnectionType

sealed class Event {
    data class Connected(val connectionType: ConnectionType) : Event()
    data class Disconnected(val exception: Throwable? = null) : Event()
    data class MessageReceived(val message: Message, private val isSelfGenerated: Boolean = false) : Event()
}