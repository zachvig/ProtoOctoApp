package de.crysxd.octoapp.widgets

import android.content.Context
import androidx.core.content.edit
import de.crysxd.octoapp.base.di.Injector
import timber.log.Timber

object WidgetPreferences {

    private val sharedPreferences by lazy { Injector.get().context().getSharedPreferences("widget_preferences", Context.MODE_PRIVATE) }

    fun setInstanceForWidgetId(widgetId: Int, webUrl: String?) = sharedPreferences.edit {
        Timber.i("Configuring widget $widgetId for instance $webUrl")
        putString("${widgetId}_webUrl", webUrl)
    }

    fun getInstanceForWidgetId(widgetId: Int) = sharedPreferences.getString("${widgetId}_webUrl", null)

    fun getWidgetIdsForInstance(webUrl: String) = sharedPreferences.all.keys.filter {
        it.endsWith("_webUrl") && sharedPreferences.getString(it, null) == webUrl
    }.map {
        it.replace("_webUrl", "").toInt()
    }

    fun deletePreferencesForWidgetId(widgetId: Int) = sharedPreferences.edit {
        Timber.i("Deleting preferences for widget $widgetId")
        sharedPreferences.all.keys.filter { it.contains(widgetId.toString()) }.forEach { remove(it) }
    }
}