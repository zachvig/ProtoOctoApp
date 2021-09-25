package de.crysxd.baseui.common.terminal

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import de.crysxd.baseui.common.ViewBindingHolder
import de.crysxd.baseui.databinding.ItemTerminalStyledCommandEndBinding
import de.crysxd.baseui.databinding.ItemTerminalStyledCommandStartBinding
import de.crysxd.baseui.databinding.ItemTerminalStyledStandardBinding
import de.crysxd.baseui.R
import de.crysxd.octoapp.base.data.models.SerialCommunication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class StyledTerminalAdapter : TerminalAdapter<RecyclerView.ViewHolder>() {

    private val serialCommunications = mutableListOf<Item>()
    private val commandStartRegex = Pattern.compile("^Send:\\s+(.*)$")
    private val commandStandardRegex = Pattern.compile("^Recv:\\s+(.*)$")
    private val commandEndRegex = Pattern.compile("^Recv:\\s+ok$")
    var activeCommand: Boolean = false

    init {
        setHasStableIds(true)
    }

    override suspend fun initWithItems(items: List<SerialCommunication>) {
        val new = withContext(Dispatchers.IO) {
            items.flatMap(this@StyledTerminalAdapter::mapItem)
        }

        withContext(Dispatchers.Main) {
            serialCommunications.clear()
            serialCommunications.addAll(new)
            notifyDataSetChanged()
        }
    }

    override suspend fun appendItem(item: SerialCommunication) = withContext(Dispatchers.Main) {
        val items = mapItem(item)
        serialCommunications.addAll(items)

        notifyItemInserted(itemCount - items.size)
    }

    private fun mapItem(item: SerialCommunication): List<Item> {
        val standardMatcher = commandStandardRegex.matcher(item.content)
        if (standardMatcher.matches()) {
            val standard = Item.Standard(standardMatcher.group(1)!!, activeCommand)
            return if (commandEndRegex.matcher(item.content).matches()) {
                activeCommand = false
                return listOf(standard, Item.CommandEnd)
            } else {
                listOf(standard)
            }
        }

        val startMatcher = commandStartRegex.matcher(item.content)
        if (startMatcher.matches()) {
            activeCommand = true
            return listOf(Item.CommandStart(startMatcher.group(1)!!))
        }

        return listOf(Item.Standard(item.content, activeCommand))
    }

    override fun clear() {
        val count = serialCommunications.size
        serialCommunications.clear()
        notifyItemRangeRemoved(0, count)
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemViewType(position: Int) = serialCommunications[position]::class.hashCode()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        Item.Standard::class.hashCode() -> ViewHolder.StandardHolder(parent)
        Item.CommandStart::class.hashCode() -> ViewHolder.CommandStartHolder(parent)
        Item.CommandEnd::class.hashCode() -> ViewHolder.CommandEndHolder(parent)
        else -> throw UnsupportedOperationException("Unknown view type $viewType")
    }

    override fun getItemCount() = serialCommunications.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = serialCommunications[position]
        holder.itemView.findViewById<TextView>(R.id.textView)?.text = item.data

        if (item is Item.Standard) {
            if (item.partOfCommand) {
                holder.itemView.setBackgroundResource(R.drawable.styled_terminal_standard_background)
            } else {
                holder.itemView.background = null
            }
        }
    }

    sealed class Item(open val data: String) {
        data class Standard(override val data: String, val partOfCommand: Boolean) : Item(data)
        data class CommandStart(override val data: String) : Item(data)
        object CommandEnd : Item("")
    }

    sealed class ViewHolder<T : ViewBinding>(binding: T) : ViewBindingHolder<T>(binding) {
        class StandardHolder(parent: ViewGroup) : ViewHolder<ItemTerminalStyledStandardBinding>(
            ItemTerminalStyledStandardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        class CommandStartHolder(parent: ViewGroup) : ViewHolder<ItemTerminalStyledCommandStartBinding>(
            ItemTerminalStyledCommandStartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        class CommandEndHolder(parent: ViewGroup) : ViewHolder<ItemTerminalStyledCommandEndBinding>(
            ItemTerminalStyledCommandEndBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }
}