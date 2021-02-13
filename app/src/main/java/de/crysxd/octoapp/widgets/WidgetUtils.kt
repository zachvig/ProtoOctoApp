package de.crysxd.octoapp.widgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import de.crysxd.octoapp.EXTRA_TARGET_OCTOPRINT_WEB_URL
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget
import java.text.DateFormat
import java.util.*

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
}