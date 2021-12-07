package de.crysxd.baseui.menu

import android.content.Context

abstract class RevolvingOptionsMenuItem : MenuItem {
    abstract val activeValue: String
    abstract val options: List<Option>
    override val canRunWithAppInBackground = false

    override fun getRightDetail(context: Context) = options.firstOrNull { it.value == activeValue }?.label
        ?: options.firstOrNull()?.label

    override suspend fun onClicked(host: MenuHost?) {
        val current = options.indexOfFirst { it.value == activeValue }.coerceAtLeast(0)
        val next = (current + 1) % options.size
        handleOptionActivated(host, options[next])
    }

    abstract fun handleOptionActivated(host: MenuHost?, option: Option)

    data class Option(
        val label: CharSequence,
        val value: String
    )
}