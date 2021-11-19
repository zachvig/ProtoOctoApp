package de.crysxd.octoapp.help.plugins

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.baseui.common.ViewBindingHolder
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.help.R
import de.crysxd.octoapp.help.databinding.HelpPluginsLibraryItemBinding

class PluginsLibraryListAdapter() : RecyclerView.Adapter<PluginsLibraryListAdapter.PluginItemViewHolder>() {

    var category: PluginsLibraryViewModel.PluginCategory? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PluginItemViewHolder(parent)

    override fun onBindViewHolder(holder: PluginItemViewHolder, position: Int) = category?.plugins?.get(position)?.let { item ->
        holder.binding.title.text = item.name
        holder.binding.description.text = item.description
        holder.binding.status.isVisible = item.installed == true || item.highlight == true
        holder.binding.status.setImageResource(if (item.installed == true) R.drawable.ic_round_verified_24 else R.drawable.ic_round_star_18)
        holder.binding.status.setColorFilter(ContextCompat.getColor(holder.itemView.context, if (item.installed == true) R.color.green else R.color.yellow))
        holder.binding.pluginPage.isVisible = item.pluginPage != null
        holder.binding.pluginPage.setOnClickListener { it.openUri(item.pluginPage) }
        holder.binding.octoappTutorial.isVisible = item.octoAppTutorial != null
        holder.binding.octoappTutorial.setOnClickListener { it.openUri(item.octoAppTutorial) }
        holder.binding.separator.isVisible = position != itemCount - 1
    } ?: Unit

    private fun View.openUri(uri: Uri?) = uri?.open(findFragment<Fragment>().requireOctoActivity(), allowCustomTabs = true)

    override fun getItemCount() = category?.plugins?.size ?: 0

    class PluginItemViewHolder(parent: ViewGroup) : ViewBindingHolder<HelpPluginsLibraryItemBinding>(
        HelpPluginsLibraryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
}

