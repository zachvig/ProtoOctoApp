package de.crysxd.octoapp.base.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import de.crysxd.octoapp.base.models.WidgetPreferences
import timber.log.Timber

class WidgetPreferencesDataSource(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) {

    fun storeOrder(listId: String, preferences: WidgetPreferences) {
        Timber.i("Storing order: $preferences")
        sharedPreferences.edit {
            putString("widget_prefs_2_$listId", gson.toJson(preferences))
        }
    }

    fun loadOrder(listId: String): WidgetPreferences? {
        val string = sharedPreferences.getString("widget_prefs_2_$listId", null)
        return gson.fromJson(string, WidgetPreferences::class.java)
    }
}
