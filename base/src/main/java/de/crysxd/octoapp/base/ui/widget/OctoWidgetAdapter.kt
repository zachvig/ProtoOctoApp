package de.crysxd.octoapp.base.ui.widget

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.common.AutoBindViewHolder
import kotlinx.android.synthetic.main.item_widget.view.*

class OctoWidgetAdapter() : RecyclerView.Adapter<OctoWidgetAdapter.WidgetViewHolder>() {

    var widgets: List<OctoWidget> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = widgets[position]::class.java.canonicalName.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = WidgetViewHolder(parent)

    override fun getItemCount() = widgets.size

    override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) {
        holder.itemView.textViewWidgetTitle.text = widgets[position].getTitle(holder.itemView.context)
        holder.itemView.widgetContainer.removeAllViews()
        holder.itemView.widgetContainer.addView(widgets[position].getView(holder.itemView.context, holder.itemView.widgetContainer))
    }

    class WidgetViewHolder(parent: ViewGroup) : AutoBindViewHolder(parent, R.layout.item_widget)

}