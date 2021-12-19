package de.crysxd.baseui.timelapse

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.crysxd.baseui.R
import de.crysxd.baseui.common.ViewBindingHolder
import de.crysxd.baseui.databinding.TimelapseArchiveItemBinding
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.ext.format
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseFile

class TimelapseArchiveAdapter(
    private val onSelected: (TimelapseFile) -> Unit,
) : RecyclerView.Adapter<TimelapseArchiveAdapter.TimelapseViewHolder>() {

    var picasso: Picasso? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var items: List<TimelapseArchiveViewModel.EnrichedTimelapseFile> = emptyList()
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
        holder.binding.title.text = item.file.name
        holder.binding.subtitle.text = when {
            item.file.recording == true -> "Recording...**"
            item.file.rendering == true -> "Rendering...**"
            else -> holder.itemView.context.getString(R.string.x_y, item.file.unixDate?.format(), item.file.bytes.asStyleFileSize())
        }
        holder.binding.progress.isVisible = item.file.rendering == true || item.file.recording == true
        if (holder.binding.progress.isVisible) {
            picasso?.cancelRequest(holder.binding.thumb)
            holder.binding.thumb.setImageDrawable(null)
        } else {
            item.thumbUri?.let {
                picasso?.load(item.thumbUri)
                    ?.error(R.drawable.ic_round_videocam_24)
                    ?.resize(200, 200)
                    ?.centerCrop()
                    ?.into(holder.binding.thumb)
            } ?: holder.binding.thumb.setImageResource(R.drawable.ic_round_videocam_24)
        }
        holder.itemView.setOnClickListener { onSelected(item.file) }
    }

    override fun getItemId(position: Int) = items[position].file.name.hashCode().toLong()

    override fun getItemCount() = items.size

    class TimelapseViewHolder(parent: ViewGroup) : ViewBindingHolder<TimelapseArchiveItemBinding>(
        TimelapseArchiveItemBinding.inflate(LayoutInflater.from(parent.context)).also { it.root.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT) }
    )
}