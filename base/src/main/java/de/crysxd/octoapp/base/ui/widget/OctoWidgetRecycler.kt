package de.crysxd.octoapp.base.ui.widget

import android.content.Context
import android.view.ViewGroup
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
            Timber.i("Inflated $widget after a delay of $delay")
            registerWidget(widget)
        }
    }

    private fun registerWidget(widget: RecyclableOctoWidget<*, *>) {
        widgetPool.getOrPut(widget::class) { mutableListOf() }.add(widget)
        Timber.i("Registered $widget")
    }

    fun <T : RecyclableOctoWidget<*, *>> rentWidget(host: WidgetHostFragment, widgetClass: KClass<T>): T {
        @Suppress("UNCHECKED_CAST")
        val widget = (widgetPool[widgetClass]?.firstOrNull {
            it.view.parent == null
        } ?: let {
            val newWidget = widgetClass.java.getConstructor(Context::class.java).newInstance(host.requireOctoActivity())
            Timber.i("Ad-hoc inflated $newWidget")
            registerWidget(newWidget)
            newWidget
        }) as T

        Timber.i("Renting out $widget")
        return widget
    }

    fun returnWidget(widget: RecyclableOctoWidget<*, *>) {
        Timber.i("Returning $widget")
        (widget.view.parent as? ViewGroup)?.removeView(widget.view)
    }
}