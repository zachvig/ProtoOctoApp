package de.crysxd.octoapp.base.repository

import de.crysxd.octoapp.base.datasource.WidgetPreferencesDataSource
import de.crysxd.octoapp.base.models.WidgetPreferences
import timber.log.Timber

class WidgetPreferencesRepository(
    private val dataSource: WidgetPreferencesDataSource
) {

    fun getWidgetOrder(listId: String): WidgetPreferences? {
        val prefs = dataSource.loadOrder(listId)
        Timber.i("Loading widget preferences for $listId: $prefs")
        return prefs
    }

    fun setWidgetOrder(listId: String, preferences: WidgetPreferences) {
        Timber.i("Updating widget preferences for $listId: $preferences")
        dataSource.storeOrder(listId, preferences)
    }

}
