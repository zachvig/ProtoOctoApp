package de.crysxd.octoapp.base.ui.widget

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.common.AutoBindViewHolder
import kotlinx.android.synthetic.main.item_widget.view.*
import kotlinx.coroutines.delay

class OctoWidgetAdapter() : RecyclerView.Adapter<OctoWidgetAdapter.WidgetViewHolder>() {

    private var widgets: List<OctoWidget> = emptyList()

    init {
        setHasStableIds(true)
    }

    suspend fun setWidgets(list: List<OctoWidget>) {
        // Delay slightly so the window animation is decoupled from laying out the widgets
        // The widgets will smoothly animate in
        delay(400)
        widgets = list
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int) = widgets[position]::class.java.canonicalName.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = WidgetViewHolder(parent)

    override fun getItemCount() = widgets.size

    override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) {
        holder.itemView.textViewWidgetTitle.text = widgets[position].getTitle(holder.itemView.context)
        holder.itemView.widgetContainer.removeAllViews()
        holder.itemView.widgetContainer.addView(widgets[position].getView(holder.itemView.context, holder.itemView.widgetContainer))
        holder.itemView.padding.isVisible = position < itemCount - 1
    }

    class WidgetViewHolder(parent: ViewGroup) : AutoBindViewHolder(parent, R.layout.item_widget)

}