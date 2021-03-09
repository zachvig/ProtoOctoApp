package de.crysxd.octoapp.base.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.common.OctoRecyclerView
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import timber.log.Timber
import java.util.*
import kotlin.reflect.KClass

class WidgetLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleRes: Int = 0
) : OctoRecyclerView(
    context,
    attrs,
    defStyleRes
), LifecycleObserver {

    private val shownWidgets = mutableListOf<RecyclableOctoWidget<*, *>>()
    private var widgetRecycler: OctoWidgetRecycler? = null
    private lateinit var currentLifecycleOwner: LifecycleOwner
    private val widgetAdapter = Adapter()
    private val widgetLayoutManager: GridLayoutManager
    var onWidgetOrderChanged: (List<KClass<out RecyclableOctoWidget<*, *>>>) -> Unit = {}

    init {
        val spanCount = resources.getInteger(R.integer.widget_list_span_count)
        val canDragSideways = spanCount > 1
        val dragDirs = if (canDragSideways) {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        } else {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        }
        widgetLayoutManager = GridLayoutManager(context, spanCount)

        adapter = widgetAdapter
        layoutManager = widgetLayoutManager
        ItemTouchHelper(ItemTouchHelperCallback(dragDirs)).attachToRecyclerView(this)
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

        widgetAdapter.notifyDataSetChanged()
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

    private inner class Adapter : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent.context)

        override fun getItemCount() = shownWidgets.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val widget = shownWidgets[position]
            holder.container.removeAllViews()
            (widget.view.parent as? ViewGroup)?.removeView(widget.view)
            holder.container.addView(shownWidgets[position].view)
            widget.view.updateLayoutParams {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    private inner class ItemTouchHelperCallback(dragDirs: Int) : ItemTouchHelper.SimpleCallback(dragDirs, 0) {

        private fun forEachWidgetButOne(viewHolder: RecyclerView.ViewHolder, action: View.() -> Unit) {
            (0 until widgetAdapter.itemCount).forEach {
                val view = widgetLayoutManager.findViewByPosition(it)
                if (view != null && view != viewHolder.itemView) {
                    action(view)
                }
            }
        }

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            forEachWidgetButOne(viewHolder) {
                animate().alpha(0.33f).scaleX(0.95f).scaleY(0.95f).start()
            }
            return super.getMovementFlags(recyclerView, viewHolder)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            Timber.i("Changed widget order")
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            Collections.swap(shownWidgets, from, to)
            widgetAdapter.notifyItemMoved(from, to)
            onWidgetOrderChanged(shownWidgets.map { it::class })
            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            forEachWidgetButOne(viewHolder) {
                animate().alpha(1f).scaleX(1f).scaleY(1f).start()
            }
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    }

    private class ViewHolder(context: Context) : RecyclerView.ViewHolder(FrameLayout(context)) {
        val container = itemView as FrameLayout

        init {
            container.layoutParams = GridLayoutManager.LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}