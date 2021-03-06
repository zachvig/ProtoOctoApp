package de.crysxd.octoapp.widgets.progress

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.asPrintTimeLeftImageResource
import de.crysxd.octoapp.base.ext.asPrintTimeLeftOriginColor
import de.crysxd.octoapp.base.ext.toBitmapWithColor
import de.crysxd.octoapp.base.ui.utils.ColorTheme
import de.crysxd.octoapp.base.ui.utils.colorTheme
import de.crysxd.octoapp.base.usecase.CreateProgressAppWidgetDataUseCase
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.widgets.*
import de.crysxd.octoapp.widgets.AppWidgetPreferences.ACTIVE_WEB_URL_MARKER
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.ref.WeakReference

class ProgressAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        notifyWidgetDataChanged()
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        AppWidgetPreferences.setWidgetDimensionsForWidgetId(appWidgetId, newOptions)
        updateLayout(appWidgetId, Injector.get().localizedContext(), appWidgetManager)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            AppWidgetPreferences.deletePreferencesForWidgetId(appWidgetId)
        }
    }

    companion object {
        private var lastUpdateJobs = mutableMapOf<String, WeakReference<Job>>()

        internal fun notifyWidgetOffline() {
            Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.let {
                notifyWidgetOffline(it.webUrl)
            }
        }

        internal fun cancelAllUpdates() {
            lastUpdateJobs.entries.toList().forEach {
                it.value.get()?.cancel()
                lastUpdateJobs.remove(it.key)
            }
        }

        internal fun notifyWidgetDataChanged(currentMessage: Message.CurrentMessage) {
            // Cancel last update job and start new one
            Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.let {
                GlobalScope.launch {
                    try {
                        val data = Injector.get().createProgressAppWidgetDataUseCase()
                            .execute(CreateProgressAppWidgetDataUseCase.Params(currentMessage = currentMessage, webUrl = it.webUrl))
                        notifyWidgetDataChanged(data)
                    } catch (e: CancellationException) {
                        Timber.i("Update cancelled")
                        return@launch
                    } catch (e: Exception) {
                        Timber.e(e)
                        notifyWidgetOffline(it.webUrl)
                    }
                }
            }
        }

        internal fun notifyWidgetDataChanged() {
            // General update, we update all instances for which there is at least one widget.
            Injector.get().octorPrintRepository().getAll().filter {
                // Do not update instances where we don't have any widgets
                getAppWidgetIdsForWebUrl(it.webUrl).isNotEmpty()
            }.forEach {
                // Cancel old update, launch update
                lastUpdateJobs[it.webUrl]?.get()?.cancel()
                val job = GlobalScope.launch {
                    try {
                        notifyWidgetLoading(it.webUrl)
                        val data = Injector.get().createProgressAppWidgetDataUseCase()
                            .execute(CreateProgressAppWidgetDataUseCase.Params(currentMessage = null, webUrl = it.webUrl))
                        notifyWidgetDataChanged(data)
                    } catch (e: CancellationException) {
                        Timber.i("Update cancelled")
                        return@launch
                    } catch (e: Exception) {
                        Timber.e(e)
                        notifyWidgetOffline(it.webUrl)
                    }
                }
                lastUpdateJobs[it.webUrl] = WeakReference(job)
            }
        }

        private fun notifyWidgetDataChanged(data: CreateProgressAppWidgetDataUseCase.Result) {
            val context = Injector.get().localizedContext()
            getAppWidgetIdsForWebUrl(data.webUrl)
                .filter { ensureWidgetExists(it) }
                .forEach {
                    updateAppWidget(context, it, data, data.webUrl)
                }
        }

        private fun notifyWidgetOffline(webUrl: String) {
            Timber.i("Widgets for instance $webUrl are offline")
            val context = Injector.get().localizedContext()
            getAppWidgetIdsForWebUrl(webUrl)
                .filter { ensureWidgetExists(it) }
                .forEach {
                    updateAppWidget(context, it, data = null, webUrl = webUrl)
                }
        }

        private fun notifyWidgetLoading(webUrl: String) {
            Timber.i("Widgets for instance $webUrl are loading")
            val context = Injector.get().localizedContext()
            getAppWidgetIdsForWebUrl(webUrl)
                .filter { ensureWidgetExists(it) }
                .forEach {
                    updateAppWidget(context, it, data = null, webUrl = webUrl, loading = true)
                }
        }

        private fun getAppWidgetIdsForWebUrl(webUrl: String): List<Int> {
            val manager = AppWidgetManager.getInstance(Injector.get().localizedContext())
            val isActiveWebUrl = Injector.get().octorPrintRepository().getActiveInstanceSnapshot()?.webUrl == webUrl
            val filter = { widgetId: Int -> manager.getAppWidgetInfo(widgetId)?.provider?.className == ProgressAppWidget::class.java.name }
            val fixed = AppWidgetPreferences.getWidgetIdsForInstance(webUrl).filter(filter)
            val dynamic = AppWidgetPreferences.getWidgetIdsForInstance(ACTIVE_WEB_URL_MARKER).filter(filter).takeIf { isActiveWebUrl }
            return listOfNotNull(fixed, dynamic).flatten()
        }

        private fun updateAppWidget(context: Context, appWidgetId: Int, data: CreateProgressAppWidgetDataUseCase.Result?, webUrl: String, loading: Boolean = false) {
            val manager = AppWidgetManager.getInstance(context)
            if (data?.isLive == true) {
                Timber.v("Updating progress widget $appWidgetId with data $data")
            } else {
                Timber.i("Updating progress widget $appWidgetId with data $data")
            }

            when {
                data != null -> updateAppWidgetForData(manager, context, appWidgetId, data)
                loading -> updateAppWidgetForLoading(manager, context, appWidgetId)
                else -> updateAppWidgetForOffline(manager, context, appWidgetId, webUrl)
            }
        }

        private fun updateAppWidgetForOffline(manager: AppWidgetManager, context: Context, appWidgetId: Int, webUrl: String) {
            val views = RemoteViews(context.packageName, R.layout.app_widget_pogress_idle)
            val text = createUpdateFailedText(context, appWidgetId)
            views.setViewVisibility(R.id.live, false)
            views.setTextViewText(R.id.title, context.getString(R.string.app_widget___no_data))
            views.setTextViewText(R.id.updatedAt, text)
            views.setOnClickPendingIntent(R.id.buttonRefresh, createUpdateIntent(context, appWidgetId))
            views.setOnClickPendingIntent(R.id.root, createLaunchAppIntent(context, webUrl))
            views.setViewVisibility(R.id.updatedAt, text.isNotBlank())
            applyScaling(appWidgetId, views)
            applyColorTheme(views, webUrl)
            applyDebugOptions(views, appWidgetId)
            manager.partiallyUpdateAppWidget(appWidgetId, views)
        }

        private fun updateAppWidgetForLoading(manager: AppWidgetManager, context: Context, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.app_widget_pogress_idle)
            views.setViewVisibility(R.id.updatedAt, true)
            views.setViewVisibility(R.id.live, false)
            views.setTextViewText(R.id.updatedAt, context.getString(R.string.app_widget___updating))
            applyScaling(appWidgetId, views)
            applyDebugOptions(views, appWidgetId)
            manager.partiallyUpdateAppWidget(appWidgetId, views)
        }

        private fun updateAppWidgetForData(manager: AppWidgetManager, context: Context, appWidgetId: Int, data: CreateProgressAppWidgetDataUseCase.Result) {
            AppWidgetPreferences.setLastUpdateTime(appWidgetId)
            val progress = data.printProgress?.let { context.getString(R.string.x_percent, it * 100).replace(" ", "") }
            val etaIndicator = ContextCompat.getDrawable(context, data.printTimeLeftOrigin.asPrintTimeLeftImageResource())
                ?.toBitmapWithColor(context, data.printTimeLeftOrigin.asPrintTimeLeftOriginColor())
            val eta = runBlocking {
                data.printTimeLeft?.let { Injector.get().formatEtaUseCase().execute(FormatEtaUseCase.Params(it.toLong(), true)) }
            }
            val views = if (data.isPrinting || data.isCancelling || data.isPaused || data.isPausing) {
                RemoteViews(context.packageName, R.layout.app_widget_pogress_active)
            } else {
                RemoteViews(context.packageName, R.layout.app_widget_pogress_idle)
            }

            views.setTextViewText(R.id.progress, progress)
            views.setTextViewText(R.id.eta, eta)
            views.setTextViewText(R.id.updatedAt, createUpdatedNowText())
            views.setTextViewText(
                R.id.title, when {
                    data.isPausing -> context.getString(R.string.print_notification___pausing_title)
                    data.isPaused -> context.getString(R.string.print_notification___paused_title)
                    data.isCancelling -> context.getString(R.string.print_notification___cancelling_title)
                    data.isPrinting -> context.getString(R.string.print_notification___printing_title)
                    data.isPrinterConnected -> context.getString(R.string.app_widget___idle)
                    else -> context.getString(R.string.app_widget___no_printer)
                }
            )
            views.setViewVisibility(R.id.updatedAt, !data.isLive)
            views.setViewVisibility(R.id.live, data.isLive)
            views.setViewVisibility(R.id.eta, !eta.isNullOrBlank())
            views.setViewVisibility(R.id.buttonResume, data.isPaused && isLargeSize(appWidgetId))
            views.setViewVisibility(R.id.buttonPause, data.isPrinting && isLargeSize(appWidgetId))
            views.setViewVisibility(R.id.buttonCancel, isLargeSize(appWidgetId))
            views.setViewVisibility(R.id.buttonRefresh, isLargeSize(appWidgetId))
            views.setBoolean(R.id.buttonPause, "setEnabled", !data.isPaused && !data.isPausing)
            views.setBoolean(R.id.buttonCancel, "setEnabled", !data.isCancelling)
            views.setOnClickPendingIntent(R.id.root, createLaunchAppIntent(context, data.webUrl))
            views.setOnClickPendingIntent(R.id.buttonRefresh, createUpdateIntent(context, appWidgetId))
            views.setOnClickPendingIntent(R.id.buttonCancel, ExecuteWidgetActionActivity.createCancelTaskPendingIntent(context))
            views.setOnClickPendingIntent(R.id.buttonPause, ExecuteWidgetActionActivity.createPauseTaskPendingIntent(context))
            views.setOnClickPendingIntent(R.id.buttonResume, ExecuteWidgetActionActivity.createResumeTaskPendingIntent(context))
            views.setImageViewBitmap(R.id.etaIndicator, etaIndicator)
            applyColorTheme(views, data.webUrl)
            applyDebugOptions(views, appWidgetId)
            applyScaling(appWidgetId, views)
            manager.updateAppWidget(appWidgetId, views)
        }

        private fun applyColorTheme(views: RemoteViews, webUrl: String) {
            val colorTheme = Injector.get().octorPrintRepository().findOrNull(webUrl)?.colorTheme ?: ColorTheme.default
            views.setInt(R.id.colorStrip, "setImageLevel", 2500)
            views.setViewVisibility(R.id.colorStrip, colorTheme != ColorTheme.default)
            views.setInt(R.id.colorStrip, "setColorFilter", colorTheme.dark)
        }

        private fun updateLayout(appWidgetId: Int, context: Context, manager: AppWidgetManager) {
            val views = RemoteViews(context.packageName, R.layout.app_widget_pogress_idle)
            applyScaling(appWidgetId, views)
            manager.partiallyUpdateAppWidget(appWidgetId, views)
        }

        private fun applyScaling(appWidgetId: Int, views: RemoteViews) {
            Timber.v("Scaling app widget for ${getWidgetWidth(appWidgetId)}")
            if (!isLargeSize(appWidgetId)) {
                views.setViewVisibility(R.id.buttonResume, false)
                views.setViewVisibility(R.id.buttonPause, false)
                views.setViewVisibility(R.id.buttonCancel, false)
            }
            views.setViewVisibility(R.id.buttonRefresh, isMediumSize(appWidgetId))
        }

        private fun isMediumSize(appWidgetId: Int) = getWidgetWidth(appWidgetId) > 250
        private fun isLargeSize(appWidgetId: Int) = getWidgetWidth(appWidgetId) > 300
    }
}