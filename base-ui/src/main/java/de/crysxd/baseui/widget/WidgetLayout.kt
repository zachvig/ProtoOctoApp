package de.crysxd.baseui.widget

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
import de.crysxd.baseui.common.OctoRecyclerView
import de.crysxd.baseui.common.ViewBindingHolder
import de.crysxd.baseui.databinding.WidgetListContainerBinding
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.R
import de.crysxd.octoapp.base.data.models.WidgetType
import timber.log.Timber

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
    private val lastWidgets = mutableListOf<Pair<RecyclableOctoWidget<*, *>, Boolean>>()
    val widgets get() = widgetAdapter.widgets.map { it.first.type to it.second }
    private var widgetRecycler: OctoWidgetRecycler? = null
    private var currentLifecycleOwner: LifecycleOwner? = null
    private val widgetAdapter = Adapter()
    private val widgetLayoutManager: LayoutManager
    private val itemTouchHelper: ItemTouchHelper
    private val itemTouchHelperCallback: ItemTouchHelperCallback
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
        require(currentLifecycleOwner == null) { "Already connected to a lifecycle, can't be reused" }
        currentLifecycleOwner?.lifecycle?.removeObserver(this)
        Timber.tag(tag).i("Binding to lifecycle: $lifecycleOwner")
        lifecycleOwner.lifecycle.addObserver(this)
        currentLifecycleOwner = lifecycleOwner
        pauseWidgets()
        resumeWidgets()
    }

    fun showWidgets(parent: BaseWidgetHostFragment, widgetClasses: Map<WidgetType, Boolean>) {
        requireNotNull(currentLifecycleOwner) { "Must call connectToLifecycle() first" }

        val changes = lastWidgets.map { it.first.type } != widgetClasses.map { it.key }
        if (!changes) {
            Timber.i("No changes in widgets, re-binding but skipping installation")
        } else {
            Timber.tag(tag).i("Installing widgets: $widgetClasses")
            val recycler = parent.requireOctoActivity().octoWidgetRecycler
            widgetRecycler = recycler
            returnAllWidgets()

            val widgets = widgetClasses.map {
                recycler.rentWidget(instanceId, parent, it.key) to it.value
            }.onEach {
                it.first.attach(parent)
            }

            lastWidgets.clear()
            lastWidgets.addAll(widgets)
            resumeWidgets()
            widgetAdapter.notifyDataSetChanged()
        }

        widgetAdapter.widgets = lastWidgets
    }

    private fun returnAllWidgets() {
        lastWidgets.forEach { it.first.onPause() }
        lastWidgets.forEach { widgetRecycler?.returnWidget(instanceId, it.first) }
        lastWidgets.clear()
    }

    @Suppress("Unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        Timber.tag(tag).i("Parent destroyed")
        disconnectFromLifecycle()
    }

    @Suppress("Unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onPause() {
        Timber.tag(tag).i("Parent paused")
        pauseWidgets()
    }

    @Suppress("Unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onResume() {
        Timber.tag(tag).i("Parent resumed")
        resumeWidgets()
    }

    private fun pauseWidgets() {
        lastWidgets.forEach { it.first.onPause() }
    }

    private fun resumeWidgets() {
        currentLifecycleOwner?.let { lc ->
            lastWidgets.forEach { it.first.onResume(lc) }
        }
    }

    private fun disconnectFromLifecycle() {
        Timber.tag(tag).i("Disconnecting from lifecycle")
        currentLifecycleOwner?.lifecycle?.removeObserver(this)
        pauseWidgets()
        returnAllWidgets()
    }

    private inner class Adapter : RecyclerView.Adapter<ViewHolder>() {


        var widgets = mutableListOf<Pair<RecyclableOctoWidget<*, *>, Boolean>>()
            set(value) {
                field = value.filter { it.first.isVisible() }.toMutableList()
                notifyDataSetChanged()
            }

        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int) = widgets[position].first.getAnalyticsName().hashCode().toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

        override fun getItemCount() = widgets.size

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val widget = widgets[position].first
            val hidden = widgets[position].second
            holder.binding.widgetContainer.removeAllViews()
            (widget.view.parent as? ViewGroup)?.removeView(widget.view)
            holder.binding.widgetContainer.addView(widget.view)
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
                    itemTouchHelperCallback.startDrag(holder)
                }
                true
            }
            holder.binding.visibilityToggle.setOnClickListener {
                val p = holder.adapterPosition
                widgets[p] = widget to !hidden
                notifyItemChanged(p)
            }
            holder.binding.visibilityToggle.setImageResource(if (hidden) R.drawable.ic_round_visibility_off_24 else R.drawable.ic_round_visibility_24)
            if (isEditMode) {
                holder.binding.widgetContainer.scaleX = 0.66f
                holder.binding.widgetContainer.scaleY = 0.66f
                holder.binding.widgetContainer.alpha = if (hidden) 0.33f else 1f
            } else {
                holder.binding.widgetContainer.alpha = 1f
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

        override fun isLongPressDragEnabled() = false

        fun startDrag(holder: ViewHolder) {
            draggedViewHolder = holder
            updateStates()
            itemTouchHelper.startDrag(holder)
        }

        fun applyState(view: View, animated: Boolean) {
            val scale = if (draggedViewHolder != null && draggedViewHolder?.itemView != view) {
                0.95f
            } else {
                1f
            }

            if (animated) {
                view.animate().scaleX(scale).scaleY(scale).start()
            } else {
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

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            Timber.i("Changed widget order")
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            val widget = widgetAdapter.widgets.removeAt(from)
            widgetAdapter.widgets.add(to, widget)
            widgetAdapter.notifyItemMoved(from, to)
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