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

internal fun updateAppWidget(context: Context, widgetId: Int) {
    val manager = AppWidgetManager.getInstance(context)
    when (val name = manager.getAppWidgetInfo(widgetId).provider.className) {
        ControlsWebcamAppWidget::class.java.name, NoControlsWebcamAppWidget::class.java.name -> BaseWebcamAppWidget.updateAppWidget(context, widgetId)
        ProgressAppWidget::class.java.name -> ProgressAppWidget.notifyWidgetDataChanged()
        else -> Timber.e(IllegalArgumentException("Supposed to update widget $widgetId with unknown provider $name"))
    }
}

internal fun updateAllWidgets() {
    BaseWebcamAppWidget.notifyWidgetDataChanged()
    ProgressAppWidget.notifyWidgetDataChanged()
}

internal fun cancelAllUpdates() {
    BaseWebcamAppWidget.cancelAllUpdates()
    ProgressAppWidget.cancelAllUpdates()
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

internal fun createUpdateIntent(context: Context, widgetId: Int, playLive: Boolean = false) =
    ExecuteWidgetActionActivity.createRefreshTaskPendingIntent(context, widgetId, playLive)

internal fun createUpdatedNowText() = getTime()

internal fun getTime() = formatTime(System.currentTimeMillis())

private fun formatDate(time: Long) = DateFormat.getDateInstance(DateFormat.SHORT).format(time)

private fun formatTime(time: Long) = DateFormat.getTimeInstance(DateFormat.SHORT).format(time)

internal fun createUpdateFailedText(context: Context, appWidgetId: Int) = AppWidgetPreferences.getLastUpdateTime(appWidgetId).takeIf { it > 0 }?.let {
    val date = formatDate(it)
    val today = formatDate(System.currentTimeMillis())
    val since = if (date == today) {
        formatTime(it)
    } else {
        date
    }
    context.getString(R.string.app_widget___offline_since_x, since)
} ?: context.getString(R.string.app_widget___update_failed)

internal fun applyDebugOptions(views: RemoteViews, appWidgetId: Int) {
    views.setTextViewText(R.id.widgetId, "$appWidgetId")
    views.setViewVisibility(R.id.widgetId, BuildConfig.DEBUG)
}

internal fun getWidgetCount(context: Context) = AppWidgetManager.getInstance(context).installedProviders.map {
    it.provider
}.filter {
    it.packageName == context.packageName
}.map {
    AppWidgetManager.getInstance(context).getAppWidgetIds(it)
}.sumBy { it.size }