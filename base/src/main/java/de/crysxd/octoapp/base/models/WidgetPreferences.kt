package de.crysxd.octoapp.base.models

data class WidgetPreferences(
    val listId: String,
    val items: List<String>,
    val hidden: List<String>,
) {

    constructor(listId: String, list: List<Pair<WidgetClass, Boolean>>) : this(
        listId,
        list.map { it.first.simpleName ?: "" },
        list.mapNotNull { it.first.simpleName.takeIf { _ -> it.second } },
    )

    fun sort(list: List<WidgetClass>): List<WidgetClass> = list.sortedBy {
        items.indexOf(it.simpleName)
    }

    fun isHidden(widget: WidgetClass) = hidden.contains(widget.simpleName)

}