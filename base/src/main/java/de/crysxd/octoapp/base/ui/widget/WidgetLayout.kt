package de.crysxd.octoapp.base.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.WidgetListContainerBinding
import de.crysxd.octoapp.base.ui.common.OctoRecyclerView
import de.crysxd.octoapp.base.ui.common.ViewBindingHolder
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import timber.log.Timber
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

    companion object {
        var instanceCount = 0
    }

    private val instanceId = instanceCount++
    private val tag = "WidgetLayout/$instanceId"
    private val shownWidgets = mutableListOf<RecyclableOctoWidget<*, *>>()
    private var widgetRecycler: OctoWidgetRecycler? = null
    private var currentLifecycleOwner: LifecycleOwner? = null
    private val widgetAdapter = Adapter()
    private val widgetLayoutManager: LayoutManager
    private val itemTouchHelper: ItemTouchHelper
    private val itemTouchHelperCallback: ItemTouchHelperCallback
    var onWidgetOrderChanged: (List<KClass<out RecyclableOctoWidget<*, *>>>) -> Unit = {}
    var isEditMode = false
        set(value) {
            field = value
            widgetAdapter.notifyDataSetChanged()
        }

    init {
        val spanCount = resources.getInteger(R.integer.widget_list_span_count)
        val canDragSideways = spanCount > 1
        val dragDirs = if (canDragSideways) {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        } else {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        }
        widgetLayoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)

        adapter = widgetAdapter
        layoutManager = widgetLayoutManager
        itemTouchHelperCallback = ItemTouchHelperCallback(dragDirs)
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(this)
    }

    fun connectToLifecycle(lifecycleOwner: LifecycleOwner) {
        currentLifecycleOwner?.lifecycle?.removeObserver(this)
        Timber.tag(tag).i("Binding to lifecycle: $lifecycleOwner")
        lifecycleOwner.lifecycle.addObserver(this)
        currentLifecycleOwner = lifecycleOwner

    }

    fun showWidgets(parent: WidgetHostFragment, widgetClasses: List<KClass<out RecyclableOctoWidget<*, *>>>) {
        Timber.tag(tag).i("Installing widgets: $widgetClasses")
        parent.requestTransition()

        val recycler = parent.requireOctoActivity().octoWidgetRecycler
        widgetRecycler = recycler

        returnAllWidgets()
        val widgets = widgetClasses.map { recycler.rentWidget(instanceId, parent, it) }.filter {
            it.isVisible()
        }.onEach {
            it.attach(parent)
        }

        shownWidgets.addAll(widgets)
        resumeWidgets()

        widgetAdapter.notifyDataSetChanged()
    }

    private fun returnAllWidgets() {
        shownWidgets.forEach { widgetRecycler?.returnWidget(instanceId, it) }
        shownWidgets.clear()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        Timber.tag(tag).i("Parent destroyed")
        disconnectFromLifecycle()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun onPause() {
        Timber.tag(tag).i("Parent paused")
        pauseWidgets()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        Timber.tag(tag).i("Parent resumed")
        resumeWidgets()
    }

    private fun pauseWidgets() {
        shownWidgets.forEach { it.onPause() }
    }

    private fun resumeWidgets() {
        currentLifecycleOwner?.let { lc ->
            shownWidgets.forEach { it.onResume(lc) }
        }
    }

    fun disconnectFromLifecycle() {
        Timber.tag(tag).i("Disconnecting from lifecycle")
        currentLifecycleOwner?.lifecycle?.removeObserver(this)
        pauseWidgets()
        returnAllWidgets()
    }

    private inner class Adapter : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

        override fun getItemCount() = shownWidgets.size

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val widget = shownWidgets[position]
            holder.binding.widgetContainer.removeAllViews()
            (widget.view.parent as? ViewGroup)?.removeView(widget.view)
            holder.binding.widgetContainer.addView(shownWidgets[position].view)
            widget.view.updateLayoutParams {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }

            // Edit mode
            holder.binding.dragHandle.isVisible = isEditMode
            holder.binding.visibilityToggle.isVisible = isEditMode
            holder.binding.widgetContainer.isChildrenTouchEnabled = !isEditMode
            holder.binding.dragHandle.setOnTouchListener { _, e ->
                if (e.action == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(holder)
                }
                true
            }
            if (isEditMode) {
                holder.binding.widgetContainer.scaleX = 0.66f
                holder.binding.widgetContainer.scaleY = 0.66f
            } else {
                holder.binding.widgetContainer.scaleX = 1f
                holder.binding.widgetContainer.scaleY = 1f
            }
            itemTouchHelperCallback.applyState(holder.itemView, false)
        }

        override fun onViewAttachedToWindow(holder: ViewHolder) {
            super.onViewAttachedToWindow(holder)
            itemTouchHelperCallback.applyState(holder.itemView, false)
        }
    }

    private inner class ItemTouchHelperCallback(dragDirs: Int) : ItemTouchHelper.SimpleCallback(dragDirs, 0) {

        private var draggedViewHolder: RecyclerView.ViewHolder? = null

        fun applyState(view: View, animated: Boolean) {
            val (alpha, scale) = if (draggedViewHolder != null && draggedViewHolder?.itemView != view) {
                0.33f to 0.95f
            } else {
                1f to 1f
            }

            if (animated) {
                view.animate().alpha(alpha).scaleX(scale).scaleY(scale).start()
            } else {
                view.alpha = alpha
                view.scaleX = scale
                view.scaleX = scale
            }
        }

        private fun updateStates() {
            (0 until widgetAdapter.itemCount).forEach {
                widgetLayoutManager.findViewByPosition(it)?.let { view ->
                    applyState(view, true)
                }
            }
        }

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            draggedViewHolder = viewHolder
            updateStates()
            return super.getMovementFlags(recyclerView, viewHolder)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            Timber.i("Changed widget order")
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            val widget = shownWidgets.removeAt(from)
            shownWidgets.add(to, widget)
            widgetAdapter.notifyItemMoved(from, to)
            onWidgetOrderChanged(shownWidgets.map { it::class })
            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            draggedViewHolder = null
            updateStates()
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    }

    private class ViewHolder(parent: ViewGroup) :
        ViewBindingHolder<WidgetListContainerBinding>(WidgetListContainerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
}