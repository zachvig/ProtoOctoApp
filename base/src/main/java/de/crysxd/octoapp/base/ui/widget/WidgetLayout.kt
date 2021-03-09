package de.crysxd.octoapp.base.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlin.reflect.KClass

class WidgetLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleRes: Int = 0
) : RecyclerView(
    context,
    attrs,
    defStyleRes
), LifecycleObserver {

    private val shownWidgets = mutableListOf<RecyclableOctoWidget<*, *>>()
    private var widgetRecycler: OctoWidgetRecycler? = null
    private lateinit var currentLifecycleOwner: LifecycleOwner

    init {
        val spanCount = resources.getInteger(R.integer.widget_list_span_count)
        layoutManager = GridLayoutManager(context, spanCount)
    }

    fun connectToLifecycle(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
        currentLifecycleOwner = lifecycleOwner
    }

    fun showWidgets(parent: WidgetHostFragment, widgetClasses: List<KClass<out RecyclableOctoWidget<*, *>>>) {
        parent.requestTransition()

        val recycler = parent.requireOctoActivity().octoWidgetRecycler
        widgetRecycler = recycler

        returnAllWidgets()
        val widgets = widgetClasses.map { recycler.rentWidget(parent, it) }.filter {
            it.isVisible()
        }.onEach {
            it.attach(parent)
        }

        shownWidgets.addAll(widgets)
        onResume()

        adapter = Adapter(widgets)
    }

    private fun returnAllWidgets() {
        shownWidgets.forEach { widgetRecycler?.returnWidget(it) }
        shownWidgets.clear()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        onPause()
        returnAllWidgets()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun onPause() {
        shownWidgets.forEach { it.onPause() }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        shownWidgets.forEach { it.onResume(currentLifecycleOwner) }
    }

    private class Adapter(widgets: List<RecyclableOctoWidget<*, *>>) : RecyclerView.Adapter<ViewHolder>() {
        private val widgets = widgets.toMutableList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent.context)

        override fun getItemCount() = widgets.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val widget = widgets[position]
            holder.container.removeAllViews()
            (widget.view.parent as? ViewGroup)?.removeView(widget.view)
            holder.container.addView(widgets[position].view)
            widget.view.updateLayoutParams {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    private class ViewHolder(context: Context) : RecyclerView.ViewHolder(FrameLayout(context)) {
        val container = itemView as FrameLayout

        init {
            container.layoutParams = GridLayoutManager.LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}