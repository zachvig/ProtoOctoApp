package de.crysxd.octoapp.base.ext

import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Message> Flow<Event>.filterEventsForMessageType(): Flow<T> = filter {
    it is Event.MessageReceived && T::class.java.isAssignableFrom(it.message::class.java)
}.map {
    (it as Event.MessageReceived).message as T
}

fun <T> Flow<T>.rateLimit(rateMs: Long): Flow<T> {
    var lastPass = 0L
    return filter {
        val pass = (System.currentTimeMillis() - lastPass) > rateMs
        if (pass) {
            lastPass = System.currentTimeMillis()
        }
        pass
    }
}