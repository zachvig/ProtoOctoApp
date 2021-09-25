package de.crysxd.baseui.widget

import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
        widget.view.tag = rentalTag
        Timber.i("Renting out $widget to $rentalTag")
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
                (widget.view.parent as? ViewGroup)?.removeView(widget.view)
            }
        } else {
            Timber.w("Couldn't accept view back, rented out from ${widget.view.tag} but returned from $rentalTag")
        }
    }
}