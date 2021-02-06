package de.crysxd.octoapp.widgets

import android.appwidget.AppWidgetManager
import android.content.*
import de.crysxd.octoapp.widgets.progress.ProgressAppWidget
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget.Companion.REFRESH_ACTION
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget.Companion.cancelAllUpdates
import de.crysxd.octoapp.widgets.webcam.ControlsWebcamAppWidget
import de.crysxd.octoapp.widgets.webcam.NoControlsWebcamAppWidget
import timber.log.Timber


class AppWidgetSupportBroadcastReceiver(context: Context) : BroadcastReceiver() {

    init {
        context.registerReceiver(this, IntentFilter().also {
            it.addAction(Intent.ACTION_SCREEN_ON)
            it.addAction(Intent.ACTION_SCREEN_OFF)
            it.addAction(REFRESH_ACTION)
        })
    }

    override fun onReceive(context: Context, intent: Intent) = when (intent.action) {
        Intent.ACTION_SCREEN_ON -> notifyWidgetDataChanged()
        Intent.ACTION_SCREEN_OFF -> cancelAllUpdates()
        REFRESH_ACTION -> intent.getIntExtra(BaseWebcamAppWidget.ARG_WIDGET_ID, 0).takeIf { it != 0 }?.let { id ->
            val live = intent.getBooleanExtra(BaseWebcamAppWidget.ARG_PLAY_LIVE, false)
            Timber.i("Updating request received for $id (live=$live)")

            val manager = AppWidgetManager.getInstance(context)
            val progressWidgetIds = ComponentName(context, ProgressAppWidget::class.java).let { manager.getAppWidgetIds(it) }
            val webcamWidgetIds = listOf(
                ComponentName(context, ControlsWebcamAppWidget::class.java).let { manager.getAppWidgetIds(it) }.toList(),
                ComponentName(context, NoControlsWebcamAppWidget::class.java).let { manager.getAppWidgetIds(it) }.toList(),
            ).flatten()

            when {
                progressWidgetIds.contains(id) -> ProgressAppWidget.notifyWidgetDataChanged()
                webcamWidgetIds.contains(id) -> BaseWebcamAppWidget.updateAppWidget(context, id, live)
            }
        } ?: notifyWidgetDataChanged()
        else -> Unit
    }

    private fun notifyWidgetDataChanged() {
        BaseWebcamAppWidget.notifyWidgetDataChanged()
        ProgressAppWidget.notifyWidgetDataChanged()
    }
}