package de.crysxd.baseui.common.terminal

import android.view.LayoutInflater
import android.view.ViewGroup
import de.crysxd.baseui.common.ViewBindingHolder
import de.crysxd.baseui.databinding.ItemPlainSerialCommBinding
import de.crysxd.octoapp.base.data.models.SerialCommunication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlainTerminalAdapter : TerminalAdapter<PlainTerminalAdapter.PlainSerialCommunicationViewHolder>() {

    private val serialCommunications = mutableListOf<SerialCommunication>()

    init {
        setHasStableIds(true)
    }

    override suspend fun initWithItems(items: List<SerialCommunication>) = withContext(Dispatchers.Main) {
        Timber.i("Init with ${items.size} items")
        serialCommunications.clear()
        serialCommunications.addAll(items)
        notifyDataSetChanged()
    }

    override suspend fun appendItem(item: SerialCommunication) = withContext(Dispatchers.Main) {
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
        holder.binding.textView.text = serialCommunications[position].content
    }

    class PlainSerialCommunicationViewHolder(parent: ViewGroup) :
        ViewBindingHolder<ItemPlainSerialCommBinding>(ItemPlainSerialCommBinding.inflate(LayoutInflater.from(parent.context), parent, false))

}