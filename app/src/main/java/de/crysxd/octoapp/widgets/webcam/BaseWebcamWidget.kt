package de.crysxd.octoapp.widgets.webcam

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import de.crysxd.octoapp.EXTRA_TARGET_OCTOPRINT_WEB_URL
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.usecase.GetWebcamSnapshotUseCase
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import de.crysxd.octoapp.widgets.WidgetPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.*

abstract class BaseWebcamWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == REFRESH_ACTION || intent.action == Intent.ACTION_SCREEN_ON) {
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
            lastUpdateJobs[appWidgetId]?.get()?.cancel()
            WidgetPreferences.deletePreferencesForWidgetId(appWidgetId)
        }
    }

    companion object {
        private const val REFRESH_ACTION = "de.crysxd.octoapp.widgets.ACTION_REFRESH"
        private const val ARG_WIDGET_ID = "widgetId"
        private const val ARG_PLAY_LIVE = "playLive"
        private const val LIVE_FOR_SECS = 30
        private const val BITMAP_WIDTH = 1080
        private var lastUpdateJobs = mutableMapOf<Int, WeakReference<Job>>()

        internal fun cancelAllUpdates() {
            lastUpdateJobs.entries.toList().forEach {
                it.value.get()?.cancel()
                lastUpdateJobs.remove(it.key)
            }
        }

        internal fun notifyWidgetDataChanged() {
            val context = Injector.get().context()
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, NoControlsWebcamWidget::class.java)).forEach {
                updateAppWidget(context, it)
            }
            manager.getAppWidgetIds(ComponentName(context, ControlsWebcamWidget::class.java)).forEach {
                updateAppWidget(context, it)
            }
        }

        internal fun updateAppWidget(context: Context, appWidgetId: Int, playLive: Boolean = false) {
            lastUpdateJobs[appWidgetId]?.get()?.cancel()
            lastUpdateJobs[appWidgetId] = WeakReference(GlobalScope.launch {
                Timber.i("Updating webcam widget $appWidgetId")

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val hasControls = appWidgetManager.getAppWidgetInfo(appWidgetId).provider.className == ControlsWebcamWidget::class.java.name
                val webUrl = WidgetPreferences.getInstanceForWidgetId(appWidgetId)
                var webCamSettings: WebcamSettings? = null

                // Load frame or do live stream
                val frame = try {
                    val octoPrintInfo = Injector.get().octorPrintRepository().getAll().firstOrNull { it.webUrl == webUrl }
                    webCamSettings = Injector.get().getWebcamSettingsUseCase().execute(octoPrintInfo)

                    // Push loading state
                    appWidgetManager.updateAppWidget(
                        appWidgetId, createViews(
                            context = context,
                            webUrl = webUrl,
                            widgetId = appWidgetId,
                            webcamSettings = webCamSettings,
                            updatedAtText = if (playLive) createLiveForText(0) else "Updating...",
                            live = false,
                            frame = null
                        )
                    )


                    if (playLive) {
                        doLiveStream(context, octoPrintInfo, webCamSettings, webUrl, appWidgetManager, appWidgetId)
                    } else {
                        createBitmapFlow(octoPrintInfo).first()
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }

                // Push loaded frame and end live stream
                val views = createViews(
                    context = context,
                    widgetId = appWidgetId,
                    webUrl = webUrl,
                    webcamSettings = webCamSettings,
                    updatedAtText = (if (frame == null) createUpdateFailedText(appWidgetId) else createUpdatedNowText()).takeIf { hasControls },
                    live = false,
                    frame = frame
                )
                views.setOnClickPendingIntent(R.id.buttonRefresh, createUpdateIntent(context, appWidgetId, false))
                views.setOnClickPendingIntent(R.id.buttonLive, createUpdateIntent(context, appWidgetId, true))
                views.setViewVisibility(R.id.buttonRefresh, if (hasControls) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.buttonLive, if (hasControls) View.VISIBLE else View.GONE)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                frame?.let {
                    WidgetPreferences.setImageDimensionsForWidgetId(appWidgetId, it.width, it.height)
                }

                if (frame != null) {
                    WidgetPreferences.setLastImageForWidgetId(appWidgetId)
                }
            })
        }

        private suspend fun doLiveStream(
            context: Context,
            octoPrintInfo: OctoPrintInstanceInformationV2?,
            webcamSettings: WebcamSettings,
            webUrl: String?,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ): Bitmap? {
            var frame: Bitmap? = null
            withTimeoutOrNull(LIVE_FOR_SECS * 1000L) {
                // Thread A: Load webcam images
                launch {
                    createBitmapFlow(octoPrintInfo).collect { frame = it }
                }

                // Thread B: Update UI every 100ms
                launch {
                    val start = System.currentTimeMillis()
                    while (true) {
                        val secsLeft = (System.currentTimeMillis() - start) / 1000
                        val views = createViews(
                            context = context,
                            widgetId = appWidgetId,
                            webUrl = webUrl,
                            webcamSettings = webcamSettings,
                            updatedAtText = createLiveForText(secsLeft.toInt()),
                            live = true,
                            frame = frame
                        )
                        views.setViewVisibility(R.id.buttonCancelLive, View.VISIBLE)
                        views.setOnClickPendingIntent(R.id.buttonCancelLive, createUpdateIntent(context, appWidgetId, false))
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        delay(100)
                    }
                }
            }
            return frame
        }

        private suspend fun createBitmapFlow(octoPrintInfo: OctoPrintInstanceInformationV2?) = Injector.get()
            .getWebcamSnapshotUseCase()
            .execute(GetWebcamSnapshotUseCase.Params(octoPrintInfo, BITMAP_WIDTH, R.dimen.widget_corner_radius))

        private fun createUpdateIntent(context: Context, widgetId: Int, playLive: Boolean) = PendingIntent.getBroadcast(
            context,
            "$widgetId$playLive".hashCode(),
            Intent(REFRESH_ACTION).also {
                if (playLive) {
                    it.putExtra(ARG_WIDGET_ID, widgetId)
                    it.putExtra(ARG_PLAY_LIVE, playLive)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        private fun createLaunchAppIntent(context: Context, webUrl: String?) = PendingIntent.getActivity(
            context,
            "launch_main_with_url_$webUrl".hashCode(),
            Intent(context, MainActivity::class.java).also {
                it.putExtra(EXTRA_TARGET_OCTOPRINT_WEB_URL, webUrl)
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        private fun getTime() = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())

        private fun createLiveForText(liveSinceSecs: Int) = "Live for ${LIVE_FOR_SECS - liveSinceSecs}s"

        private fun createUpdatedNowText() = "Updated at ${getTime()}"

        private fun createUpdateFailedText(appWidgetId: Int) = WidgetPreferences.getLastImageForWidgetId(appWidgetId).takeIf { it > 0 }?.let {
            "Update failed, last image at ${DateFormat.getTimeInstance(DateFormat.SHORT).format(it)}"
        } ?: "Update failed"

        private fun createViews(
            context: Context,
            widgetId: Int,
            webUrl: String?,
            webcamSettings: WebcamSettings?,
            updatedAtText: String?,
            live: Boolean,
            frame: Bitmap?
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.webcam_widget)
            frame?.let {
                views.setImageViewBitmap(R.id.webcamContent, it)
            } ?: run {
                // This generated bitmap will ensure the widget has it's final dimension and layout.
                // We set it to a separate view as the webcamContent might already have a "real" image we don't know about
                views.setImageViewBitmap(R.id.webcamContentPlaceholder, generateImagePlaceHolder(widgetId))
            }
            views.setTextViewText(R.id.noImageUrl, webcamSettings?.streamUrl)
            views.setTextViewText(R.id.updatedAt, updatedAtText)
            views.setTextViewText(R.id.live, updatedAtText)
            views.setViewVisibility(R.id.updatedAt, if (live) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.live, if (live) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.buttonCancelLive, View.GONE)
            views.setViewVisibility(R.id.buttonRefresh, View.GONE)
            views.setViewVisibility(R.id.buttonLive, View.GONE)
            views.setViewVisibility(R.id.updatedAt, if (updatedAtText.isNullOrBlank()) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.noImageCont, if (frame == null) View.VISIBLE else View.GONE)
            views.setOnClickPendingIntent(R.id.root, createLaunchAppIntent(context, webUrl))
            return views
        }

        private fun generateImagePlaceHolder(widgetId: Int) = WidgetPreferences.getImageDimensionsForWidgetId(widgetId).let {
            Bitmap.createBitmap(it.first, it.second, Bitmap.Config.ARGB_8888)
        }
    }
}