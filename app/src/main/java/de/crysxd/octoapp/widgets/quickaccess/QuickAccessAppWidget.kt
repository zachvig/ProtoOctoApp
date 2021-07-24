package de.crysxd.octoapp.widgets.quickaccess

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import timber.log.Timber


class QuickAccessAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        notifyWidgetDataChanged()
    }

    companion object {
        fun notifyWidgetDataChanged() {
            Timber.i("Updating QuickAccessAppWidget")
            val context = Injector.get().localizedContext()
            val manager = AppWidgetManager.getInstance(context)
            val widgetIds = manager.getAppWidgetIds(ComponentName(context, QuickAccessAppWidget::class.java))
            val title = Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.label?.let {
                context.getString(R.string.app_widget___controlling_x, it)
            }

            widgetIds.forEach {
                val views = RemoteViews(context.packageName, R.layout.app_widget_quick_access)
                val intent = Intent(context, QuickAccessRemoteViewsService::class.java)
                views.setRemoteAdapter(R.id.list, intent)
                views.setTextViewText(R.id.header, title)
                manager.updateAppWidget(it, views)
                manager.notifyAppWidgetViewDataChanged(it, R.id.list)
            }
        }
    }
}