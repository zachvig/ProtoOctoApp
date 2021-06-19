package de.crysxd.octoapp.base.ui.menu

import android.content.Context

abstract class RevolvingOptionsMenuItem : MenuItem {
    abstract val activeValue: String
    abstract val options: List<Option>
    abstract val isEnabled: Boolean

    override suspend fun getRightDetail(context: Context) = options.firstOrNull { it.value == activeValue }?.label
        ?: options.firstOrNull()?.label

    override suspend fun onClicked(host: MenuBottomSheetFragment?) {
        val current = options.indexOfFirst { it.value == activeValue }.coerceAtLeast(0)
        val next = (current + 1) % options.size
        handleOptionActivated(options[next])
    }

    abstract fun handleOptionActivated(option: Option)

    data class Option(
        val label: CharSequence,
        val value: String
    )
}