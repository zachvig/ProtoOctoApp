package de.crysxd.octoapp.base.ui.common.terminal

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.SerialCommunication
import de.crysxd.octoapp.base.ui.common.AutoBindViewHolder
import kotlinx.android.synthetic.main.item_plain_serial_comm.*
import timber.log.Timber

class PlainTerminalAdaper : RecyclerView.Adapter<PlainTerminalAdaper.PlainSerialCommunicationViewHolder>() {

    private val serialCommunications = mutableListOf<SerialCommunication>()

    init {
        setHasStableIds(true)
    }

    fun initWithItems(items: List<SerialCommunication>) {
        Timber.i("Init with ${items.size} items")
        serialCommunications.clear()
        serialCommunications.addAll(items)
        notifyDataSetChanged()
    }

    fun appendItem(item: SerialCommunication) {
        Timber.i("Append")
        serialCommunications.add(item)
        notifyItemInserted(itemCount - 1)
    }

    fun clear() {
        val count = serialCommunications.size
        serialCommunications.clear()
        notifyItemRangeRemoved(0, count)
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PlainSerialCommunicationViewHolder(parent)

    override fun getItemCount() = serialCommunications.size

    override fun onBindViewHolder(holder: PlainSerialCommunicationViewHolder, position: Int) {
        holder.textView.text = serialCommunications[position].content
    }

    class PlainSerialCommunicationViewHolder(parent: ViewGroup) : AutoBindViewHolder(parent, R.layout.item_plain_serial_comm)

}