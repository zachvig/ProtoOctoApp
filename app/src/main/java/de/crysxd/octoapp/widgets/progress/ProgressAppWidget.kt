package de.crysxd.octoapp.widgets.progress

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.asPrintTimeLeftOriginColor
import de.crysxd.octoapp.base.usecase.CreateProgressAppWidgetDataUseCase
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.widgets.AppWidgetPreferences
import de.crysxd.octoapp.widgets.createLaunchAppIntent
import de.crysxd.octoapp.widgets.createUpdateIntent
import de.crysxd.octoapp.widgets.createUpdatedNowText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.lang.ref.WeakReference

class ProgressAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        notifyWidgetDataChanged()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            lastUpdateJobs[appWidgetId]?.get()?.cancel()
            AppWidgetPreferences.deletePreferencesForWidgetId(appWidgetId)
        }
    }

    companion object {
        internal const val REFRESH_ACTION = "de.crysxd.octoapp.widgets.ACTION_REFRESH"
        internal const val ARG_WIDGET_ID = "widgetId"
        private var lastUpdateJobs = mutableMapOf<Int, WeakReference<Job>>()

        internal fun notifyWidgetDataChanged(currentMessage: Message.CurrentMessage) {
            Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.let {
                GlobalScope.launch {
                    try {
                        val data = Injector.get().createProgressAppWidgetDataUseCase()
                            .execute(CreateProgressAppWidgetDataUseCase.Params(currentMessage = currentMessage, webUrl = it.webUrl))
                        notifyWidgetDataChanged(data)
                    } catch (e: Exception) {
                        Timber.e(e)
                        // TODO push error
                    }
                }
            }
        }

        internal fun notifyWidgetDataChanged() {
            Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.let {
                GlobalScope.launch {
                    try {
                        val data = Injector.get().createProgressAppWidgetDataUseCase()
                            .execute(CreateProgressAppWidgetDataUseCase.Params(currentMessage = null, webUrl = it.webUrl))
                        notifyWidgetDataChanged(data)
                    } catch (e: Exception) {
                        Timber.e(e)
                        // TODO push error
                    }
                }
            }
        }

        internal fun notifyWidgetDataChanged(data: CreateProgressAppWidgetDataUseCase.Result) {
            val context = Injector.get().context()
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, ProgressAppWidget::class.java)).forEach {
                updateAppWidget(context, it, data)
            }
        }

        internal fun updateAppWidget(context: Context, appWidgetId: Int, data: CreateProgressAppWidgetDataUseCase.Result) {
            Timber.i("Updating progress widget $appWidgetId with data $data")
            val manager = AppWidgetManager.getInstance(context)
            val progress = data.printProgress?.let { context.getString(R.string.x_percent, it * 100).replace(" ", "") }
            val eta = runBlocking {
                data.printTimeLeft?.let { Injector.get().formatEtaUseCase().execute(it) }
            }
            val etaIndicatorColor = data.printTimeLeftOrigin.asPrintTimeLeftOriginColor()
            val views = if (data.isPrinting || data.isCancelling || data.isPaused || data.isPausing) {
                RemoteViews(context.packageName, R.layout.app_widget_pogress_active_normal)
            } else {
                RemoteViews(context.packageName, R.layout.app_widget_pogress_idle_normal)
            }

            views.setTextViewText(R.id.progress, progress)
            views.setTextViewText(R.id.eta, context.getString(R.string.eta_x, eta))
            views.setTextViewText(R.id.updatedAt, createUpdatedNowText())
            views.setTextViewText(
                R.id.title, when {
                    data.isPausing -> context.getString(R.string.notification_pausing_title)
                    data.isPaused -> context.getString(R.string.notification_paused_title)
                    data.isCancelling -> context.getString(R.string.notification_cancelling_title)
                    data.isPrinting -> context.getString(R.string.notification_printing_title)
                    else -> ""
                }
            )
            views.setViewVisibility(R.id.updatedAt, if (data.isLive) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.live, if (data.isLive) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.eta, if (eta.isNullOrBlank()) View.GONE else View.VISIBLE)
            views.setOnClickPendingIntent(R.id.root, createLaunchAppIntent(context, data.webUrl))
            views.setOnClickPendingIntent(R.id.buttonRefresh, createUpdateIntent(context, appWidgetId))
            views.setInt(R.id.etaIndicator, "setColorFilter", ContextCompat.getColor(context, etaIndicatorColor))
            manager.updateAppWidget(appWidgetId, views)
        }
    }
}