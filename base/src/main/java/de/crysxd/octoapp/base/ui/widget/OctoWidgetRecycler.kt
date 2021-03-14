package de.crysxd.octoapp.base.ui.widget

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.ui.base.OctoActivity
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.reflect.KClass

class OctoWidgetRecycler {

    private val widgetPool = mutableMapOf<KClass<out RecyclableOctoWidget<*, *>>, MutableList<RecyclableOctoWidget<*, *>>>()

    fun preInflateWidget(activity: OctoActivity, factory: () -> RecyclableOctoWidget<*, *>) {
        activity.lifecycleScope.launchWhenCreated {
            val delay = (0..500L).random()
            delay(delay)
            val widget = factory()
            widget.view.tag = -1
            Timber.i("Inflated $widget after a delay of $delay")
            registerWidget(widget)
        }
    }

    private fun registerWidget(widget: RecyclableOctoWidget<*, *>) {
        widgetPool.getOrPut(widget::class) { mutableListOf() }.add(widget)
        Timber.i("Registered $widget")
    }

    fun <T : RecyclableOctoWidget<*, *>> rentWidget(rentalTag: Int, host: Fragment, widgetClass: KClass<T>): T {
        @Suppress("UNCHECKED_CAST")
        val widget = findIdleWidget(widgetClass) ?: let {
            val newWidget = widgetClass.java.getConstructor(Context::class.java).newInstance(host.requireOctoActivity())
            Timber.i("Ad-hoc inflated $newWidget")
            registerWidget(newWidget)
            newWidget
        }

        widget.view.tag = rentalTag
        Timber.i("Renting out $widget to $rentalTag")
        return widget
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : RecyclableOctoWidget<*, *>> findIdleWidget(widgetClass: KClass<T>): T? = widgetPool[widgetClass]?.firstOrNull {
        it.view.tag == -1
    } as? T?

    fun returnWidget(rentalTag: Int, widget: RecyclableOctoWidget<*, *>) {
        if (widget.view.tag == rentalTag) {

            if (findIdleWidget(widget::class) != null) {
                widgetPool[widget::class]?.remove(widget)
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