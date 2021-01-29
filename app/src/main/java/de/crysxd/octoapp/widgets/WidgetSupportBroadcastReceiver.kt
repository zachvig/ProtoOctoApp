package de.crysxd.octoapp.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.crysxd.octoapp.widgets.webcam.WebcamWidget.Companion.cancelAllUpdates
import de.crysxd.octoapp.widgets.webcam.WebcamWidget.Companion.notifyWidgetDataChanged


class WidgetSupportBroadcastReceiver(context: Context) : BroadcastReceiver() {

    init {
        context.registerReceiver(this, IntentFilter().also {
            it.addAction(Intent.ACTION_SCREEN_ON)
            it.addAction(Intent.ACTION_SCREEN_OFF)
        })
    }

    override fun onReceive(context: Context, intent: Intent) = when (intent.action) {
        Intent.ACTION_SCREEN_ON -> notifyWidgetDataChanged()
        Intent.ACTION_SCREEN_OFF -> cancelAllUpdates()
        else -> Unit
    }
}