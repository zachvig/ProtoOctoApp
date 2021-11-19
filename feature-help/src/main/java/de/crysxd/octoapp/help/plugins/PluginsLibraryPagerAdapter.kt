package de.crysxd.octoapp.help.plugins

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.baseui.common.ViewBindingHolder
import de.crysxd.octoapp.help.databinding.HelpPluginsLibraryPageBinding

class PluginsLibraryPagerAdapter() : RecyclerView.Adapter<PluginsLibraryPagerAdapter.PluginListViewHolder>() {

    var index: PluginsLibraryViewModel.PluginsIndex? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PluginListViewHolder(parent)

    override fun onBindViewHolder(holder: PluginListViewHolder, position: Int) {
        holder.adapter.category = index?.categories?.get(position)
    }

    override fun getItemCount() = index?.categories?.size ?: 0

    class PluginListViewHolder(parent: ViewGroup) : ViewBindingHolder<HelpPluginsLibraryPageBinding>(
        HelpPluginsLibraryPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    ) {
        val adapter = PluginsLibraryListAdapter()

        init {
            binding.root.adapter = adapter
        }
    }
}

