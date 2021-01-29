package de.crysxd.octoapp.widgets.webcam

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.usecase.GetWebcamSnapshotUseCase
import de.crysxd.octoapp.widgets.WidgetPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DateFormat
import java.util.*

const val REFRESH_ACTION = "de.crysxd.octoapp.widgets.ACTION_REFRESH"
const val ARG_WIDGET_ID = "widgetId"
const val ARG_PLAY_LIVE = "playLive"

class WebcamWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == REFRESH_ACTION) {
            intent.getIntExtra(ARG_WIDGET_ID, 0).takeIf { it != 0 }?.let {
                val live = intent.getBooleanExtra(ARG_PLAY_LIVE, false)
                Timber.i("Updating request received for $it (live=$live)")
                updateAppWidget(context, it, live)
            } ?: notifyWidgetDataChanged()
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetId)
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

internal fun notifyWidgetDataChanged() {
    val context = Injector.get().context()
    val manager = AppWidgetManager.getInstance(context)
    manager.getAppWidgetIds(ComponentName(context, WebcamWidget::class.java)).forEach {
        updateAppWidget(context, it)
    }
}

internal fun updateAppWidget(context: Context, appWidgetId: Int, playLive: Boolean = false) = GlobalScope.launch {
    Timber.i("Updating widget $appWidgetId")

    val appWidgetManager = AppWidgetManager.getInstance(context)
    val webUrl = WidgetPreferences.getInstanceForWidgetId(appWidgetId)
    val octoPrintInfo = Injector.get().octorPrintRepository().getAll().firstOrNull { it.webUrl == webUrl }
    val webCamSettings = Injector.get().getWebcamSettingsUseCase().execute(octoPrintInfo)
    val liveForSecs = 15

    fun createLiveForText(liveSinceSecs: Int) = "Live for ${liveForSecs - liveSinceSecs}s"

    fun createViews(updatedAtText: String): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.webcam_widget)
        views.setTextViewText(R.id.noImageUrl, webCamSettings.streamUrl)
        views.setTextViewText(R.id.updatedAt, updatedAtText)
        views.setViewVisibility(R.id.buttonRefresh, View.GONE)
        views.setViewVisibility(R.id.buttonLive, View.GONE)
        return views
    }

    appWidgetManager.updateAppWidget(appWidgetId, createViews(if (playLive) createLiveForText(0) else "Updating..."))

    suspend fun pushFrame(views: RemoteViews) = try {
        val frame = Injector.get().getWebcamSnapshotUseCase().execute(GetWebcamSnapshotUseCase.Params(octoPrintInfo, 1080, R.dimen.widget_corner_radius))
        views.setImageViewBitmap(R.id.webcamContent, frame)
        true
    } catch (e: Exception) {
        Timber.e(e)
        views.setTextViewText(R.id.noImageTitle, "Error while loading image")
        false
    }

    if (playLive) {
        repeat(liveForSecs * 2) {
            val start = System.currentTimeMillis()
            val views = createViews(createLiveForText(it / 2))
            if (!pushFrame(views)) {
                return@repeat
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
            delay(500 - (System.currentTimeMillis() - start))
        }
    }

    val views = createViews("Updated at ${getTime()}")
    pushFrame(views)
    views.setOnClickPendingIntent(R.id.buttonRefresh, createUpdateIntent(context, appWidgetId, false))
    views.setOnClickPendingIntent(R.id.buttonLive, createUpdateIntent(context, appWidgetId, true))
    views.setViewVisibility(R.id.buttonRefresh, View.VISIBLE)
    views.setViewVisibility(R.id.buttonLive, View.VISIBLE)
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun createUpdateIntent(context: Context, widgetId: Int, playLive: Boolean) = PendingIntent.getBroadcast(
    context,
    "$widgetId$playLive".hashCode(),
    Intent(REFRESH_ACTION).also {
        it.putExtra(ARG_WIDGET_ID, widgetId)
        it.putExtra(ARG_PLAY_LIVE, playLive)
    },
    PendingIntent.FLAG_UPDATE_CURRENT
)

private fun getTime() = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
