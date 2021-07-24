package de.crysxd.octoapp.widgets.quickaccess

import android.content.Intent
import android.widget.RemoteViewsService

internal class QuickAccessRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return QuickAccessRemoteViewsFactory(applicationContext)
    }
}