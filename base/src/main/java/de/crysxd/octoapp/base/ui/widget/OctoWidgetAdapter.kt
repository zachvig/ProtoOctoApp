package de.crysxd.octoapp.base.ui.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import kotlinx.android.synthetic.main.item_widget.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class OctoWidgetAdapter : RecyclerView.Adapter<OctoWidgetAdapter.WidgetViewHolder>() {

    private var widgets: List<Pair<Int, WidgetViewHolder>> = emptyList()
    private var dataSetId = 0

    init {
        setHasStableIds(true)
    }

    fun dispatchResume() {
        widgets.forEach { it.second.widget.onResume() }
    }

    fun dispatchPause() {
        widgets.forEach { it.second.widget.onPause() }
    }

    suspend fun setWidgets(context: Context, list: List<OctoWidget>) {
        if (widgets.map { it.second.widgetClassName } == list.map { it::class.java.name }) {
            Timber.i("Widgets not changed, skipping update")
        } else {
            dataSetId++

            widgets = list.mapIndexed { i, it ->
                onCreateAndBindViewHolder(context, it, i, list.size)
            }.mapIndexed { index, widgetViewHolder ->
                "$dataSetId:$index".hashCode() to widgetViewHolder
            }

            withContext(Dispatchers.Main) {
                notifyDataSetChanged()
            }
        }
    }

    private suspend fun onCreateAndBindViewHolder(context: Context, widget: OctoWidget, position: Int, count: Int) =
        WidgetViewHolder(context, widget::class.java.name, widget).also { holder ->
            LayoutInflater.from(context).suspendedInflate(R.layout.item_widget, holder.itemView as ViewGroup, true)

            holder.itemView.textViewWidgetTitle.text = widget.getTitle(holder.itemView.context)
            holder.itemView.textViewWidgetTitle.isVisible = holder.itemView.textViewWidgetTitle.text.isNotBlank()
            holder.itemView.padding.isVisible = position < count - 1
            holder.itemView.imageButtonSettings.visibility = if (widget.hasSettings()) View.VISIBLE else View.INVISIBLE
            holder.itemView.imageButtonSettings.setOnClickListener {
                if (widget.hasSettings()) {
                    widget.showSettings()
                }
            }

            holder.itemView.widgetContainer.removeAllViews()
            holder.itemView.widgetContainer.addView(widget.getView(context, holder.itemView.widgetContainer))

            return holder
        }

    override fun getItemId(position: Int) = widgets[position].second::class.java.canonicalName.hashCode().toLong()

    override fun getItemViewType(position: Int) = widgets[position].first

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = widgets.first { it.first == viewType }.second.also {
        (it.itemView.parent as? ViewGroup)?.removeView(it.itemView)
    }

    override fun getItemCount() = widgets.size

    override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) = Unit

    class WidgetViewHolder(context: Context, val widgetClassName: String, val widget: OctoWidget) : RecyclerView.ViewHolder(FrameLayout(context)) {
        init {
            setIsRecyclable(false)
        }
    }
}