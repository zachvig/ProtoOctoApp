package de.crysxd.baseui.common.terminal

import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.data.models.SerialCommunication

abstract class TerminalAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    abstract suspend fun initWithItems(items: List<SerialCommunication>)
    abstract suspend fun appendItem(item: SerialCommunication)
    abstract fun clear()
}