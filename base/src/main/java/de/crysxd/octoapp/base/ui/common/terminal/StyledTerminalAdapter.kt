package de.crysxd.octoapp.base.ui.common.terminal

import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.SerialCommunication
import de.crysxd.octoapp.base.ui.common.AutoBindViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.regex.Pattern

class StyledTerminalAdapter : TerminalAdapter<StyledTerminalAdapter.ViewHolder>() {

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
        Timber.i("Append ${item.content}")
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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

    sealed class ViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : AutoBindViewHolder(parent, layout) {
        class StandardHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.item_terminal_styled_standard)
        class CommandStartHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.item_terminal_styled_command_start)
        class CommandEndHolder(parent: ViewGroup) : ViewHolder(parent, R.layout.item_terminal_styled_command_end)
    }
}