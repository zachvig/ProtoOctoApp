package de.crysxd.octoapp.base.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WidgetPreferences(
    val listId: String,
    val items: List<WidgetType>,
    val hidden: List<WidgetType>,
) : Parcelable {

    constructor(listId: String, list: List<Pair<WidgetType, Boolean>>) : this(
        listId,
        list.map { it.first },
        list.mapNotNull { it.first.takeIf { _ -> it.second } },
    )

    fun prepare(list: List<WidgetType>) = list.sortedBy {
        items.indexOf(it).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }.map {
        it to hidden.contains(it)
    }.toMap()
}
