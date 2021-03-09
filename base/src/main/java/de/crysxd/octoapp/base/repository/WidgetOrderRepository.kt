package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.WidgetOrderDataSource
import de.crysxd.octoapp.base.models.WidgetOrder

class WidgetOrderRepository(
    private val dataSource: WidgetOrderDataSource
) {

    fun getWidgetOrder(listId: String) = dataSource.load(listId)

    fun setWidgetOrder(listId: String, order: WidgetOrder) = dataSource.store(listId, order)
}