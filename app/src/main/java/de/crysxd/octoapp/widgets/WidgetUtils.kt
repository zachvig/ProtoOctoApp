package de.crysxd.octoapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.crysxd.octoapp.BuildConfig
import de.crysxd.octoapp.EXTRA_TARGET_OCTOPRINT_ID
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.billing.BillingManager.FEATURE_QUICK_SWITCH
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.format
import de.crysxd.octoapp.base.utils.PendingIntentCompat
import de.crysxd.octoapp.widgets.progress.ProgressAppWidget
import de.crysxd.octoapp.widgets.quickaccess.QuickAccessAppWidget
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget
import de.crysxd.octoapp.widgets.webcam.ControlsWebcamAppWidget
import de.crysxd.octoapp.widgets.webcam.NoControlsWebcamAppWidget
import timber.log.Timber
import java.util.Date

internal fun updateAppWidget(widgetId: Int) {
    val context = BaseInjector.get().localizedContext()
    val manager = AppWidgetManager.getInstance(context)
    when (val name = manager.getAppWidgetInfo(widgetId).provider.className) {
        ControlsWebcamAppWidget::class.java.name, NoControlsWebcamAppWidget::class.java.name -> BaseWebcamAppWidget.updateAppWidget(widgetId)
        ProgressAppWidget::class.java.name -> ProgressAppWidget.notifyWidgetDataChanged()
        else -> Timber.e(IllegalArgumentException("Supposed to update widget $widgetId with unknown provider $name"))
    }
}

internal fun ensureWidgetExists(widgetId: Int) = AppWidgetManager.getInstance(BaseInjector.get().context()).getAppWidgetInfo(widgetId) != null

internal fun updateAllWidgets() {
    BaseWebcamAppWidget.notifyWidgetDataChanged()
    ProgressAppWidget.notifyWidgetDataChanged()
    QuickAccessAppWidget.notifyWidgetDataChanged()
}

internal fun cancelAllUpdates() {
    BaseWebcamAppWidget.cancelAllUpdates()
    ProgressAppWidget.cancelAllUpdates()
}

internal fun createLaunchAppIntent(context: Context, instanceId: String?) = PendingIntent.getActivity(
    context,
    "launch_main_with_instance_$instanceId".hashCode(),
    Intent(context, MainActivity::class.java).also {
        if (BillingManager.isFeatureEnabled(FEATURE_QUICK_SWITCH)) {
            it.putExtra(EXTRA_TARGET_OCTOPRINT_ID, instanceId)
        }
    },
    PendingIntentCompat.FLAG_UPDATE_CURRENT_IMMUTABLE
)

internal fun createUpdateIntent(context: Context, widgetId: Int, playLive: Boolean = false) =
    ExecuteWidgetActionActivity.createRefreshTaskPendingIntent(context, widgetId, playLive)

internal fun createUpdatedNowText() = getTime()

internal fun getTime() = Date().format()

internal fun createUpdateFailedText(context: Context, appWidgetId: Int) = AppWidgetPreferences.getLastUpdateTime(appWidgetId).takeIf { it > 0 }?.let {
    context.getString(R.string.app_widget___offline_since_x, Date(it).format())
} ?: context.getString(R.string.app_widget___update_failed)

internal fun applyDebugOptions(views: RemoteViews, appWidgetId: Int) {
    views.setTextViewText(R.id.widgetId, "$appWidgetId/${AppWidgetPreferences.getInstanceForWidgetId(appWidgetId)}")
    views.setViewVisibility(R.id.widgetId, BuildConfig.DEBUG)
}

internal fun getWidgetWidth(appWidgetId: Int) = AppWidgetPreferences.getWidgetDimensionsForWidgetId(appWidgetId).first

internal fun getWidgetHeight(appWidgetId: Int) = AppWidgetPreferences.getWidgetDimensionsForWidgetId(appWidgetId).second

internal fun getWidgetCount(context: Context) = AppWidgetManager.getInstance(context).installedProviders.map {
    it.provider
}.filter {
    it.packageName == context.packageName
}.map {
    AppWidgetManager.getInstance(context).getAppWidgetIds(it)
}.map {
    it.filter { ensureWidgetExists(it) }
}.sumOf { it.size }