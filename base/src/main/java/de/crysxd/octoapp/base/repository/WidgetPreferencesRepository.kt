package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.WidgetPreferencesDataSource
import de.crysxd.octoapp.base.models.WidgetPreferences

class WidgetPreferencesRepository(
    private val dataSource: WidgetPreferencesDataSource
) {

    fun getWidgetOrder(listId: String) = dataSource.loadOrder(listId)

    fun setWidgetOrder(listId: String, preferences: WidgetPreferences) = dataSource.storeOrder(listId, preferences)

}
