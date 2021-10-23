package de.crysxd.baseui.widget

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import de.crysxd.baseui.OctoActivity
import de.crysxd.octoapp.base.data.models.WidgetType
import kotlinx.coroutines.delay
import timber.log.Timber

class OctoWidgetRecycler {

    private val widgetPool = mutableMapOf<WidgetType, MutableList<RecyclableOctoWidget<*, *>>>()
    private lateinit var widgetFactory: (WidgetType) -> RecyclableOctoWidget<*, *>

    fun setWidgetFactory(activity: OctoActivity, factory: (WidgetType) -> RecyclableOctoWidget<*, *>) {
        widgetFactory = factory

        WidgetType.values().forEach {
            if (findIdleWidget(it) == null) {
                activity.lifecycleScope.launchWhenCreated {
                    val delay = (0L..500L).random()
                    delay(delay)
                    Timber.i("Inflated $it after a delay of $delay")
                }
            }
        }
    }

    private fun createWidget(widgetType: WidgetType): RecyclableOctoWidget<*, *> {
        val widget = widgetFactory(widgetType)
        widget.view.tag = -1
        widgetPool.getOrPut(widget.type) { mutableListOf() }.add(widget)
        Timber.i("Registered $widget")
        return widget
    }

    @Suppress("UNCHECKED_CAST")
    fun rentWidget(rentalTag: Int, host: Fragment, widgetType: WidgetType): RecyclableOctoWidget<*, *> {
        val widget = findIdleWidget(widgetType) ?: createWidget(widgetType)
        (widget.view.parent as? ViewGroup)?.removeView(widget.view)
        widget.view.tag = rentalTag
        Timber.i("Renting out $widget to $rentalTag")

        // We also set up an "insurance". If the host destroys the view, we will detach the rented widget
        // if it's not returned or was returned but not rented out again
        host.viewLifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onViewDestroyed() {
                if (widget.view.tag == rentalTag || widget.view.tag == -1) {
                    Timber.tag("OctoWidget/Insurance").i("Host of rental of view $widget with tag $rentalTag was destroyed, detaching view")
                    (widget.view.parent as? ViewGroup)?.removeView(widget.view)
                }
            }
        })

        return widget
    }

    private fun findIdleWidget(widgetType: WidgetType): RecyclableOctoWidget<*, *>? = widgetPool[widgetType]?.firstOrNull {
        it.view.tag == -1
    }

    fun returnWidget(rentalTag: Int, widget: RecyclableOctoWidget<*, *>) {
        if (widget.view.tag == rentalTag) {

            if (findIdleWidget(widget.type) != null) {
                widgetPool[widget.type]?.remove(widget)
                Timber.i("Already one widget of type ${widget::class} in pool, destroying $widget")
            } else {
                Timber.i("Returning $widget from $rentalTag")
                widget.view.tag = -1
            }
        } else {
            Timber.w("Couldn't accept view back, rented out from ${widget.view.tag} but returned from $rentalTag")
        }
    }
}