package de.crysxd.octoapp.base.ui.common.terminal

import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.models.SerialCommunication

abstract class TerminalAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    abstract suspend fun initWithItems(items: List<SerialCommunication>)
    abstract suspend fun appendItem(item: SerialCommunication)
    abstract fun clear()
}