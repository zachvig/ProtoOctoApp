package de.crysxd.octoapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.crysxd.octoapp.BuildConfig
import de.crysxd.octoapp.EXTRA_TARGET_OCTOPRINT_WEB_URL
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.widgets.progress.ProgressAppWidget
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget
import de.crysxd.octoapp.widgets.webcam.ControlsWebcamAppWidget
import de.crysxd.octoapp.widgets.webcam.NoControlsWebcamAppWidget
import timber.log.Timber
import java.text.DateFormat
import java.util.*

internal fun updateAppWidget(context: Context, widgetId: Int) {
    val manager = AppWidgetManager.getInstance(context)
    when (val name = manager.getAppWidgetInfo(widgetId).provider.className) {
        ControlsWebcamAppWidget::class.java.name, NoControlsWebcamAppWidget::class.java.name -> BaseWebcamAppWidget.updateAppWidget(context, widgetId)
        ProgressAppWidget::class.java.name -> ProgressAppWidget.notifyWidgetDataChanged()
        else -> Timber.e(IllegalArgumentException("Supposed to update widget $widgetId with unknown provider $name"))
    }
}

internal fun createLaunchAppIntent(context: Context, webUrl: String?) = PendingIntent.getActivity(
    context,
    "launch_main_with_url_$webUrl".hashCode(),
    Intent(context, MainActivity::class.java).also {
        if (BillingManager.isFeatureEnabled("quick_switch")) {
            it.putExtra(EXTRA_TARGET_OCTOPRINT_WEB_URL, webUrl)
        }
    },
    PendingIntent.FLAG_UPDATE_CURRENT
)

internal fun createUpdateIntent(context: Context, widgetId: Int, playLive: Boolean = false) = PendingIntent.getBroadcast(
    context,
    "$widgetId$playLive".hashCode(),
    Intent(BaseWebcamAppWidget.REFRESH_ACTION).also {
        if (playLive) {
            it.putExtra(BaseWebcamAppWidget.ARG_WIDGET_ID, widgetId)
            it.putExtra(BaseWebcamAppWidget.ARG_PLAY_LIVE, playLive)
        }
    },
    PendingIntent.FLAG_UPDATE_CURRENT
)

internal fun createUpdatedNowText() = "Updated at ${getTime()}"

internal fun getTime() = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())

internal fun createUpdateFailedText(appWidgetId: Int) = AppWidgetPreferences.getLastUpdateTime(appWidgetId).takeIf { it > 0 }?.let {
    "Offline since ${DateFormat.getTimeInstance(DateFormat.SHORT).format(it)}"
} ?: "Update failed"

internal fun applyDebugOptions(views: RemoteViews, appWidgetId: Int) {
    views.setTextViewText(R.id.widgetId, "$appWidgetId")
    views.setViewVisibility(R.id.widgetId, BuildConfig.DEBUG)
}