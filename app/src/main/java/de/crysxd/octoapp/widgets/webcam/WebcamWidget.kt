package de.crysxd.octoapp.widgets.webcam

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.widgets.WidgetPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber


class WebcamWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            WidgetPreferences.deletePreferencesForWidgetId(appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) = GlobalScope.launch {
    val webUrl = WidgetPreferences.getInstanceForWidgetId(appWidgetId)
    val octoPrintInfo = Injector.get().octorPrintRepository().getAll().firstOrNull { it.webUrl == webUrl }
    val webCamSettings = Injector.get().getWebcamSettingsUseCase().execute(octoPrintInfo)

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.webcam_widget)
    views.setTextViewText(R.id.noImageUrl, webCamSettings.streamUrl)

    try {


    } catch (e: Exception) {
        Timber.e(e)
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
