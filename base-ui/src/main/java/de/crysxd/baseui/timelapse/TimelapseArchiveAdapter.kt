package de.crysxd.baseui.timelapse

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.baseui.R
import de.crysxd.baseui.common.ViewBindingHolder
import de.crysxd.baseui.databinding.TimelapseArchiveItemBinding
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.ext.format
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseFile

class TimelapseArchiveAdapter : RecyclerView.Adapter<TimelapseArchiveAdapter.TimelapseViewHolder>() {

    var items: List<TimelapseFile> = emptyList()
        set(value) {
            notifyItemRangeRemoved(0, field.size)
            field = value
            notifyItemRangeInserted(0, field.size)
        }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TimelapseViewHolder((parent))

    override fun onBindViewHolder(holder: TimelapseViewHolder, position: Int) {
        val item = items[position]
        holder.binding.title.text = item.name
        holder.binding.subtitle.text = when {
            item.recording == true -> "Recording...**"
            item.rendering == true -> "Rendering...**"
            else -> holder.itemView.context.getString(R.string.x_y, item.unixDate?.format(), item.bytes.asStyleFileSize())
        }
        holder.binding.progress.isVisible = item.rendering == true || item.recording == true
        if (holder.binding.progress.isVisible) {
            holder.binding.thumb.setImageDrawable(null)
        } else {
            holder.binding.thumb.setImageResource(R.drawable.ic_round_videocam_24)
        }
    }

    override fun getItemId(position: Int) = items[position].let { it.name + it.date + it.bytes }.hashCode().toLong()

    override fun getItemCount() = items.size

    class TimelapseViewHolder(parent: ViewGroup) : ViewBindingHolder<TimelapseArchiveItemBinding>(
        TimelapseArchiveItemBinding.inflate(LayoutInflater.from(parent.context)).also { it.root.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT) }
    )
}