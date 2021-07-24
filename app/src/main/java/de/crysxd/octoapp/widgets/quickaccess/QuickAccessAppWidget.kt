package de.crysxd.octoapp.widgets.quickaccess

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.MenuId
import de.crysxd.octoapp.widgets.*
import timber.log.Timber


class QuickAccessAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        notifyWidgetDataChanged()
    }

    override fun onAppWidgetOptionsChanged(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        AppWidgetPreferences.setWidgetDimensionsForWidgetId(appWidgetId, newOptions)
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
            val isEmpty = Injector.get().pinnedMenuItemsRepository().getPinnedMenuItems(MenuId.Widget).isEmpty()

            widgetIds.forEach {
                val isLarge = getWidgetHeight(it) > 120
                val layout = if (isEmpty) R.layout.app_widget_quick_access_empty else R.layout.app_widget_quick_access
                val views = RemoteViews(context.packageName, layout)
                val intent = Intent(context, QuickAccessRemoteViewsService::class.java)
                views.setRemoteAdapter(R.id.list, intent)
                views.setPendingIntentTemplate(R.id.list, ExecuteWidgetActionActivity.createClickMenuItemPendingIntentTemplate(context))
                views.setTextViewText(R.id.footer, title)
                views.setViewVisibility(R.id.footer, isLarge)
                views.setViewVisibility(R.id.footerLine, isLarge)
                views.setOnClickPendingIntent(R.id.root, createLaunchAppIntent(context, null))
                views.setInt(R.id.root, "setBackgroundResource", if (isLarge) R.drawable.widget_background_flat_top else R.drawable.widget_background_small)
                manager.updateAppWidget(it, views)
                manager.notifyAppWidgetViewDataChanged(it, R.id.list)
            }
        }
    }
}