package de.crysxd.octoapp.help.tutorials

import android.content.Context
import android.text.Spannable
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import de.crysxd.baseui.common.ViewBindingHolder
import de.crysxd.octoapp.help.R
import de.crysxd.octoapp.help.databinding.TutorialItemBinding
import io.noties.markwon.SpannableBuilder
import java.util.Date
import kotlin.math.absoluteValue

class TutorialsAdapter(
    context: Context,
    val onSelected: (YoutubePlaylist.PlaylistItem) -> Unit
) : RecyclerView.Adapter<TutorialsAdapter.TutorialAdapterViewHolder>() {

    private val picasso = Picasso.Builder(context).build()
    private val newBadgeSpan = ImageSpan(context, R.drawable.ic_new)
    var data: Data = Data(lastOpened = Date(0), tutorials = emptyList())
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TutorialAdapterViewHolder(parent)

    override fun onBindViewHolder(holder: TutorialAdapterViewHolder, position: Int) {
        val item = data.tutorials[position]
        val isNew = data.lastOpened < item.contentDetails?.videoPublishedAt ?: Date(0)
        val doubleNewLineIndex = item.snippet?.description?.indexOf("\n\n")?.takeIf { it > 0 } ?: Int.MAX_VALUE
        holder.binding.header.isVisible = position == 0
        holder.binding.title.text = item.snippet?.title?.removePrefix("OctoApp Tutorials:")?.trim()?.let {
            if (isNew) {
                SpannableBuilder("  $it").setSpan(newBadgeSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE).text()
            } else {
                it
            }
        }
        holder.binding.description.text = item.snippet?.description?.take(doubleNewLineIndex)
        holder.binding.root.setOnClickListener { onSelected(item) }
        holder.binding.root.doOnLayout {
            // Find thumbnail fitting the preview size best
            item.snippet?.thumbnails?.minByOrNull {
                (holder.binding.thumbnail.width - it.value.width).absoluteValue
            }?.let {
                picasso.load(it.value.url).into(holder.binding.thumbnail)
            }
        }
    }

    override fun getItemCount() = data.tutorials.size

    data class Data(
        val lastOpened: Date,
        val tutorials: List<YoutubePlaylist.PlaylistItem>,
    )

    class TutorialAdapterViewHolder(parent: ViewGroup) : ViewBindingHolder<TutorialItemBinding>(
        TutorialItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
}