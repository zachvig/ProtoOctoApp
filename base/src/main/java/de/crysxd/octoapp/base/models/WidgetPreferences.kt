package de.crysxd.octoapp.base.models

import android.os.Parcelable
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import kotlinx.parcelize.Parcelize
import kotlin.reflect.KClass

@Parcelize
data class WidgetPreferences(
    val listId: String,
    val items: List<String>,
    val hidden: List<String>,
) : Parcelable {

    constructor(listId: String, list: List<Pair<WidgetClass, Boolean>>) : this(
        listId,
        list.map { it.first.simpleName ?: "" },
        list.mapNotNull { it.first.simpleName.takeIf { _ -> it.second } },
    )

    fun prepare(list: List<WidgetClass>) = list.sortedBy {
        items.indexOf(it.simpleName)
    }.map {
        it to (hidden.contains(it.simpleName) ?: false)
    }.toMap()

}