package de.crysxd.octoapp.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.core.content.edit
import de.crysxd.octoapp.base.di.Injector
import timber.log.Timber

object AppWidgetPreferences {

    const val ACTIVE_WEB_URL_MARKER = "active"

    private val sharedPreferences by lazy { Injector.get().context().getSharedPreferences("widget_preferences", Context.MODE_PRIVATE) }

    fun setInstanceForWidgetId(widgetId: Int, webUrl: String?) = sharedPreferences.edit {
        Timber.i("Configuring widget $widgetId for instance $webUrl")
        putString("${widgetId}_webUrl", webUrl)
    }

    fun getInstanceForWidgetId(widgetId: Int) = sharedPreferences.getString("${widgetId}_webUrl", null).takeIf { it != ACTIVE_WEB_URL_MARKER }

    fun getWidgetIdsForInstance(webUrl: String?) = sharedPreferences.all.keys.filter {
        it.endsWith("_webUrl") && sharedPreferences.getString(it, null) == webUrl
    }.map {
        it.replace("_webUrl", "").toInt()
    }

    fun setImageDimensionsForWidgetId(widgetId: Int, width: Int, height: Int) = sharedPreferences.edit {
        putInt("${widgetId}_image_width", width)
        putInt("${widgetId}_image_height", height)
    }

    fun setWidgetDimensionsForWidgetId(widgetId: Int, newOptions: Bundle) = sharedPreferences.edit {
        putInt("${widgetId}_width", newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 1))
        putInt("${widgetId}_height", newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 1))
    }

    fun getImageDimensionsForWidgetId(widgetId: Int) = Pair(
        sharedPreferences.getInt("${widgetId}_image_width", 0),
        sharedPreferences.getInt("${widgetId}_image_height", 0)
    )

    fun getWidgetDimensionsForWidgetId(widgetId: Int) = Pair(
        sharedPreferences.getInt("${widgetId}_width", 0),
        sharedPreferences.getInt("${widgetId}_height", 0)
    )

    fun setLastUpdateTime(widgetId: Int) = sharedPreferences.edit {
        putLong("${widgetId}_last_update_time", System.currentTimeMillis())
    }

    fun getLastUpdateTime(widgetId: Int) =
        sharedPreferences.getLong("${widgetId}_last_update_time", 0)

    fun deletePreferencesForWidgetId(widgetId: Int) = sharedPreferences.edit {
        Timber.i("Deleting preferences for widget $widgetId")
        sharedPreferences.all.keys.filter { it.contains(widgetId.toString()) }.forEach { remove(it) }
    }
}