package de.crysxd.octoapp.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.core.content.edit
import de.crysxd.octoapp.base.di.Injector
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

object AppWidgetPreferences {

    const val ACTIVE_INSTANCE_MARKER = "active"

    private val sharedPreferences by lazy { Injector.get().context().getSharedPreferences("widget_preferences", Context.MODE_PRIVATE) }

    init {
        // Upgrade from webUrl to instance id
        sharedPreferences.edit {
            sharedPreferences.all.keys.filter {
                it.endsWith("_webUrl")
            }.map {
                // Try to get the istance id, fall back to active instance marker if not found
                val webUrl = sharedPreferences.getString(it, null) ?: return@map it to ACTIVE_INSTANCE_MARKER
                val instance = Injector.get().octorPrintRepository().findInstancesWithWebUrl(webUrl.toHttpUrlOrNull()) ?: return@map it to ACTIVE_INSTANCE_MARKER
                it to instance.id
            }.forEach {
                // Remove web url field and store instanceId field
                Timber.i("Upgrading from ${it.first} ot ${it.second}")
                remove(it.first)
                putString(it.first.replace("_webUrl", "_instanceId"), it.second)
            }
        }
    }

    fun setInstanceForWidgetId(widgetId: Int, instanceId: String = ACTIVE_INSTANCE_MARKER) = sharedPreferences.edit {
        Timber.i("Configuring widget $widgetId for instance $instanceId")
        putString("${widgetId}_instanceId", instanceId)
    }

    fun getInstanceForWidgetId(widgetId: Int) = sharedPreferences.getString("${widgetId}_instanceId", null).takeIf { it != ACTIVE_INSTANCE_MARKER }

    fun getWidgetIdsForInstance(instanceId: String) = sharedPreferences.all.keys.filter {
        it.endsWith("_instanceId") && sharedPreferences.getString(it, null) == instanceId
    }.map {
        it.replace("_instanceId", "").toInt()
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