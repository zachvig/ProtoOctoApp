package de.crysxd.octoapp.base.ui.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import kotlinx.android.synthetic.main.item_widget.view.*
import kotlinx.coroutines.CoroutineScope

class OctoWidgetAdapter(
    private val coroutineScope: CoroutineScope
) : RecyclerView.Adapter<OctoWidgetAdapter.WidgetViewHolder>() {

    private var widgets: List<WidgetViewHolder> = emptyList()

    init {
        setHasStableIds(true)
    }

    suspend fun setWidgets(context: Context, list: List<OctoWidget>) {
        widgets = list.mapIndexed { i, it ->
            onCreateAndBindViewHolder(context, it, i, list.size)
        }
        notifyDataSetChanged()
    }

    private suspend fun onCreateAndBindViewHolder(context: Context, widget: OctoWidget, position: Int, count: Int) = WidgetViewHolder(context).also { holder ->
        LayoutInflater.from(context).suspendedInflate(R.layout.item_widget, holder.itemView as ViewGroup, true)

        holder.itemView.textViewWidgetTitle.text = widget.getTitle(holder.itemView.context)
        holder.itemView.textViewWidgetTitle.isVisible = holder.itemView.textViewWidgetTitle.text.isNotBlank()
        holder.itemView.padding.isVisible = position < count - 1

        holder.itemView.widgetContainer.removeAllViews()
        holder.itemView.widgetContainer.addView(widget.getView(context, holder.itemView.widgetContainer))

        return holder
    }

    override fun getItemId(position: Int) = widgets[position]::class.java.canonicalName.hashCode().toLong()

    override fun getItemViewType(position: Int) = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = widgets[viewType].also {
        (it.itemView.parent as? ViewGroup)?.removeView(it.itemView)
    }

    override fun getItemCount() = widgets.size

    override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) = Unit

    class WidgetViewHolder(context: Context) : RecyclerView.ViewHolder(FrameLayout(context))
}