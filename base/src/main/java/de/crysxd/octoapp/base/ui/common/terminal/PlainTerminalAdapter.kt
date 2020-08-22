package de.crysxd.octoapp.base.ui.common.terminal

import android.view.ViewGroup
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.SerialCommunication
import de.crysxd.octoapp.base.ui.common.AutoBindViewHolder
import kotlinx.android.synthetic.main.item_plain_serial_comm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlainTerminalAdaper : TerminalAdapter<PlainTerminalAdaper.PlainSerialCommunicationViewHolder>() {

    private val serialCommunications = mutableListOf<SerialCommunication>()

    init {
        setHasStableIds(true)
    }

    override suspend fun initWithItems(items: List<SerialCommunication>) {
        withContext(Dispatchers.IO) {
            Timber.i("Init with ${items.size} items")
            serialCommunications.clear()
            serialCommunications.addAll(items)
        }
        notifyDataSetChanged()
    }

    override fun appendItem(item: SerialCommunication) {
        serialCommunications.add(item)
        notifyItemInserted(itemCount - 1)
    }

    override fun clear() {
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