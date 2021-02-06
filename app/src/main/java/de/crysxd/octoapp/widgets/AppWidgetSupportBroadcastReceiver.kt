package de.crysxd.octoapp.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget.Companion.REFRESH_ACTION
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget.Companion.cancelAllUpdates
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget.Companion.notifyWidgetDataChanged
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
        REFRESH_ACTION -> intent.getIntExtra(BaseWebcamAppWidget.ARG_WIDGET_ID, 0).takeIf { it != 0 }?.let {
            val live = intent.getBooleanExtra(BaseWebcamAppWidget.ARG_PLAY_LIVE, false)
            Timber.i("Updating request received for $it (live=$live)")
            BaseWebcamAppWidget.updateAppWidget(context, it, live)
        } ?: notifyWidgetDataChanged()
        else -> Unit
    }
}