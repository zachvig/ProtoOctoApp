package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.WidgetOrderDataSource
import de.crysxd.octoapp.base.models.WidgetPreferences

class WidgetOrderRepository(
    private val dataSource: WidgetOrderDataSource
) {

    fun getWidgetOrder(listId: String) = dataSource.loadOrder(listId)

    fun setWidgetOrder(listId: String, preferences: WidgetPreferences) = dataSource.storeOrder(listId, preferences)

}
