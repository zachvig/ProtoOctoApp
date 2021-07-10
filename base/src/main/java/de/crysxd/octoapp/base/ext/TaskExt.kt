package de.crysxd.octoapp.base.ext

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.channels.Channel

suspend fun <T> Task<T>.suspendedAwait(): T {
    val channel = Channel<T>(0)
    addOnCompleteListener {
        it.exception?.let { e -> throw e }
        channel.offer(it.result)
    }
    return channel.receive()
}