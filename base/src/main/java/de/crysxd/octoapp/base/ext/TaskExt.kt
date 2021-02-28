package de.crysxd.octoapp.base.ext

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.channels.Channel

suspend fun Task<*>.suspendedAwait() {
    val channel = Channel<Unit>(0)
    addOnCompleteListener {
        it.exception?.let { e -> throw e }
        channel.offer(Unit)
    }
    channel.receive()
}