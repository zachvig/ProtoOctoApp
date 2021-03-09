package de.crysxd.octoapp.base.models

import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import kotlin.reflect.KClass

data class WidgetOrder(
    val listId: String,
    val order: List<String>
) {

    constructor(listId: String, list: List<KClass<out RecyclableOctoWidget<*, *>>>, nothing: Any = Unit) : this(
        listId,
        list.map { it.simpleName ?: "" }
    )

    fun sort(list: List<KClass<out RecyclableOctoWidget<*, *>>>): List<KClass<out RecyclableOctoWidget<*, *>>> = list.sortedBy {
        order.indexOf(it.simpleName)
    }
}