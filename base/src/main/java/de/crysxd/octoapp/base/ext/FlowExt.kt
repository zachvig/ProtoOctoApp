package de.crysxd.octoapp.base.ext

import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

@Suppress("UNCHECKED_CAST")
fun <T : Message> Flow<Event>.filterEventsForMessageType(): Flow<T> = mapNotNull {
    ((it as? Event.MessageReceived)?.message) as? T
}