package de.crysxd.octoapp.base.ui.common.terminal

import de.crysxd.octoapp.base.models.SerialCommunication

interface TerminalAdaper {
    fun initWithItems(items: List<SerialCommunication>)
    fun appendItem(item: SerialCommunication)
    fun clear()
}