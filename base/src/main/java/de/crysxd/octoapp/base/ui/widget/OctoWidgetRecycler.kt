package de.crysxd.octoapp.base.ui.widget

import android.content.Context
import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.reflect.KClass

class OctoWidgetRecycler {

    private val widgetPool = mutableMapOf<KClass<out RecyclableOctoWidget<*, *>>, MutableList<RecyclableOctoWidget<*, *>>>()

    fun preInflateWidget(factory: () -> RecyclableOctoWidget<*, *>) {
        GlobalScope.launch {
            val widget = try {
                withContext(Dispatchers.Default) {
                    val w = factory()
                    Timber.i("Async inflated $w")
                    w
                }
            } catch (e: Exception) {
                val w = factory()
                Timber.i("Sync inflated $w")
                w
            }
            registerWidget(widget)
        }
    }

    private fun registerWidget(widget: RecyclableOctoWidget<*, *>) {
        widgetPool.getOrPut(widget::class) { mutableListOf() }.add(widget)
        Timber.i("Registered $widget")
    }

    fun <T : RecyclableOctoWidget<*, *>> rentWidget(context: Context, widgetClass: KClass<T>): T {
        val widget = (widgetPool[widgetClass]?.firstOrNull {
            it.view.parent == null
        } ?: let {
            val newWidget = widgetClass.java.getConstructor(Context::class.java).newInstance(context)
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