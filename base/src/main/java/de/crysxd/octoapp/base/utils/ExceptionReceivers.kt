package de.crysxd.octoapp.base.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

object ExceptionReceivers {

    private val receivers = mutableListOf<(Throwable) -> Unit>()

    fun registerReceiver(lifecycleOwner: LifecycleOwner, receiver: (Throwable) -> Unit) {
        receivers.add(receiver)
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                receivers.remove(receiver)
            }
        })
    }

    fun dispatchException(throwable: Throwable): Boolean {
        receivers.forEach { it(throwable) }
        return receivers.isNotEmpty()
    }
}