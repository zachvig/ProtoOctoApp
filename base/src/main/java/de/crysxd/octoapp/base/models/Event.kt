package de.crysxd.octoapp.base.models

data class Event<T>(private val internalValue: T) {

    var isConsumed = false
        private set

    val value: T?
        get() = if (isConsumed) {
            null
        } else {
            isConsumed = true
            internalValue
        }
}