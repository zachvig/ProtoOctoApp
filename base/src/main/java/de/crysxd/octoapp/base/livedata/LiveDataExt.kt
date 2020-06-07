package de.crysxd.octoapp.base.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message

object OctoTransformations {

    fun <T> LiveData<T>.filter(f: (T) -> Boolean): LiveData<T> {
        val result: MediatorLiveData<T> = MediatorLiveData<T>()
        result.addSource(this) { x ->
            if (f(x)) {
                result.value = x
            }
        }
        return result
    }

    fun <T : Any> LiveData<Any>.filterForType(type: Class<T>): LiveData<T> {
        val result: MediatorLiveData<T> = MediatorLiveData<T>()
        result.addSource(this) { x ->
            if (type.isAssignableFrom(x::class.java)) {
                @Suppress("UNCHECKED_CAST")
                result.value = x as T
            }
        }
        return result
    }

    fun <T : Message> LiveData<Event>.filterEventsForMessageType(type: Class<T>): LiveData<T> {
        val result: MediatorLiveData<T> = MediatorLiveData<T>()
        result.addSource(this) { x ->
            if (x is Event.MessageReceived && type.isAssignableFrom(x.message::class.java)) {
                @Suppress("UNCHECKED_CAST")
                result.value = x.message as T
            }
        }
        return result
    }

    fun <X, Y> LiveData<X>.map(mapFunction: (X) -> Y): LiveData<Y> =
        Transformations.map(this, mapFunction)

}