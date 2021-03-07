package de.crysxd.octoapp.base.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlin.reflect.KClass

class WidgetLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleRes: Int = 0
) : LinearLayoutCompat(
    context,
    attrs,
    defStyleRes
), LifecycleObserver {

    private val shownWidgets = mutableListOf<RecyclableOctoWidget<*, *>>()
    private var widgetRecycler: OctoWidgetRecycler? = null
    private lateinit var currentLifecycleOwner: LifecycleOwner

    init {
        orientation = VERTICAL
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
        val widgets = widgetClasses.map { recycler.rentWidget(parent, it) }
        shownWidgets.addAll(widgets)

        shownWidgets.forEach {
            it.attach(parent)
            addView(it.view)
            it.view.isVisible = it.isVisible()
            it.view.updateLayoutParams {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    private fun returnAllWidgets() {
        shownWidgets.forEach { widgetRecycler?.returnWidget(it) }
        shownWidgets.clear()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
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
}