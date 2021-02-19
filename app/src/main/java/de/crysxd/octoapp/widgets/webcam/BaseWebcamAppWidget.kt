package de.crysxd.octoapp.widgets.webcam

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.usecase.GetWebcamSnapshotUseCase
import de.crysxd.octoapp.octoprint.models.settings.WebcamSettings
import de.crysxd.octoapp.widgets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

abstract class BaseWebcamAppWidget : AppWidgetProvider() {

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(Injector.get().localizedContext(), intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        AppWidgetPreferences.setWidgetDimensionsForWidgetId(appWidgetId, newOptions)
        updateLayout(appWidgetId, Injector.get().localizedContext(), appWidgetManager)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            lastUpdateJobs[appWidgetId]?.get()?.cancel()
            AppWidgetPreferences.deletePreferencesForWidgetId(appWidgetId)
        }
    }

    companion object {
        private const val LIVE_FOR_MS = 30_000L
        private const val FETCH_TIMEOUT_MS = 10_000L
        private const val BITMAP_WIDTH = 1080
        private var lastUpdateJobs = mutableMapOf<Int, WeakReference<Job>>()

        internal fun cancelAllUpdates() {
            lastUpdateJobs.entries.toList().forEach {
                it.value.get()?.cancel()
                lastUpdateJobs.remove(it.key)
            }
        }

        internal fun notifyWidgetDataChanged() {
            cancelAllUpdates()

            val context = Injector.get().localizedContext()
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, NoControlsWebcamAppWidget::class.java)).forEach {
                updateAppWidget(it)
            }
            manager.getAppWidgetIds(ComponentName(context, ControlsWebcamAppWidget::class.java)).forEach {
                updateAppWidget(it)
            }
        }

        private fun showUpdating(context: Context, appWidgetId: Int) {
            Timber.i("Applying updating state to $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.app_widget_webcam)
            views.setViewVisibility(R.id.updatedAt, true)
            views.setViewVisibility(R.id.live, false)
            views.setImageViewBitmap(R.id.webcamContentPlaceholder, generateImagePlaceHolder(appWidgetId))
            views.setTextViewText(R.id.updatedAt, context.getString(R.string.app_widget___updating))
            AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(appWidgetId, views)
        }

        private fun showFailed(context: Context, appWidgetId: Int, webUrl: String?) {
            Timber.i("Applying failed state to $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.app_widget_webcam)
            views.setViewVisibility(R.id.updatedAt, true)
            views.setViewVisibility(R.id.live, false)
            views.setImageViewBitmap(R.id.webcamContentPlaceholder, generateImagePlaceHolder(appWidgetId))
            views.setTextViewText(R.id.updatedAt, createUpdateFailedText(context, appWidgetId))
            views.setOnClickPendingIntent(R.id.buttonRefresh, createUpdateIntent(context, appWidgetId))
            views.setOnClickPendingIntent(R.id.root, createLaunchAppIntent(context, webUrl))
            AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(appWidgetId, views)
        }

        internal fun updateAppWidget(appWidgetId: Int, playLive: Boolean = false) {
            lastUpdateJobs[appWidgetId]?.get()?.cancel()
            lastUpdateJobs[appWidgetId] = WeakReference(GlobalScope.launch {
                Timber.i("Updating webcam widget $appWidgetId")

                val context = Injector.get().localizedContext()
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val hasControls = appWidgetManager.getAppWidgetInfo(appWidgetId).provider.className == ControlsWebcamAppWidget::class.java.name
                val webUrl = AppWidgetPreferences.getInstanceForWidgetId(appWidgetId)
                var webCamSettings: WebcamSettings? = null

                // Load frame or do live stream
                try {
                    val octoPrintInfo = Injector.get().octorPrintRepository().let { repo ->
                        repo.getAll().firstOrNull { it.webUrl == webUrl }
                            ?: repo.getActiveInstanceSnapshot()
                            ?: throw IllegalStateException("Unable to find info")
                    }

                    webCamSettings = Injector.get().getWebcamSettingsUseCase().execute(octoPrintInfo)

                    // Push loading state
                    showUpdating(context, appWidgetId)

                    val frame = withContext(Dispatchers.IO) {
                        if (playLive) withTimeoutOrNull(LIVE_FOR_MS) {
                            doLiveStream(context, octoPrintInfo, webCamSettings, webUrl, appWidgetManager, appWidgetId)
                        } else withTimeout(FETCH_TIMEOUT_MS) {
                            createBitmapFlow(octoPrintInfo, appWidgetId, context).first()
                        }
                    }

                    // Push loaded frame and end live stream
                    val views = createViews(
                        context = context,
                        widgetId = appWidgetId,
                        webUrl = webUrl,
                        webcamSettings = webCamSettings,
                        updatedAtText = (if (frame == null) createUpdateFailedText(context, appWidgetId) else createUpdatedNowText()).takeIf { hasControls },
                        live = false,
                        frame = frame
                    )
                    views.setOnClickPendingIntent(R.id.buttonRefresh, createUpdateIntent(context, appWidgetId, false))
                    views.setOnClickPendingIntent(R.id.buttonLive, createUpdateIntent(context, appWidgetId, true))
                    views.setViewVisibility(R.id.buttonRefresh, hasControls)
                    views.setViewVisibility(R.id.buttonLive, hasControls)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    frame?.let {
                        AppWidgetPreferences.setImageDimensionsForWidgetId(appWidgetId, it.width, it.height)
                    }

                    if (frame != null) {
                        AppWidgetPreferences.setLastUpdateTime(appWidgetId)
                    }

                } catch (e: CancellationException) {
                    Timber.i("Update cancelled")
                } catch (e: Exception) {
                    Timber.e(e)
                    showFailed(context, appWidgetId, webUrl)
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
        ): Bitmap? = withContext(Dispatchers.IO) {
            var frame: Bitmap? = null
            val lock = ReentrantLock()
            val sampleRateMs = 1000L
            // Thread A: Load webcam images
            launch {
                createBitmapFlow(octoPrintInfo, sampleRateMs = sampleRateMs, appWidgetId = appWidgetId, context = context).collect {
                    Timber.v("Received frame")
                    lock.withLock { frame = it }
                }
            }

            // Thread B: Update UI every 100ms
            launch {
                val start = System.currentTimeMillis()
                while (true) {
                    val savedFrame = lock.withLock { frame } ?: continue // No frame yet, we keep "Updating..."
                    val secsLeft = (System.currentTimeMillis() - start) / 1000
                    val views = createViews(
                        context = context,
                        widgetId = appWidgetId,
                        webUrl = webUrl,
                        webcamSettings = webcamSettings,
                        updatedAtText = createLiveForText(context, secsLeft.toInt()),
                        live = true,
                        frame = savedFrame
                    )
                    views.setViewVisibility(R.id.buttonCancelLive, true)
                    views.setOnClickPendingIntent(R.id.buttonCancelLive, createUpdateIntent(context, appWidgetId, false))
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    Timber.v("Pushed frame")
                    delay(sampleRateMs)
                }
            }
            return@withContext frame
        }

        private suspend fun createBitmapFlow(octoPrintInfo: OctoPrintInstanceInformationV2?, appWidgetId: Int, context: Context, sampleRateMs: Long = 1) =
            Injector.get().getWebcamSnapshotUseCase().execute(
                GetWebcamSnapshotUseCase.Params(
                    instanceInfo = octoPrintInfo,
                    maxWidthPx = BITMAP_WIDTH,
                    sampleRateMs = sampleRateMs,
                    cornerRadiusPx = calculateCornerRadius(context, appWidgetId)
                )
            )

        private fun calculateCornerRadius(context: Context, appWidgetId: Int): Float {
            val displayMetrics = context.resources.displayMetrics
            val widgetSize = AppWidgetPreferences.getWidgetDimensionsForWidgetId(appWidgetId).let {
                Pair(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, it.first.toFloat(), displayMetrics),
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, it.second.toFloat(), displayMetrics),
                )
            }
            val imageSize = AppWidgetPreferences.getImageDimensionsForWidgetId(appWidgetId)
            val cornerRadiusDp = context.resources.getDimension(R.dimen.widget_corner_radius)

            if (listOf(widgetSize.first, widgetSize.second, imageSize.first, imageSize.second).any { it == 0 }) {
                return cornerRadiusDp
            }

            val widthScale = imageSize.first / widgetSize.first
            val heightScale = imageSize.second / widgetSize.second
            return max(widthScale, heightScale) * cornerRadiusDp
        }

        private fun createLiveForText(context: Context, liveSinceSecs: Int) = context.getString(R.string.app_widget___live_for_x, (LIVE_FOR_MS / 1000) - liveSinceSecs)

        private fun createViews(
            context: Context,
            widgetId: Int,
            webUrl: String?,
            webcamSettings: WebcamSettings?,
            updatedAtText: String?,
            live: Boolean,
            frame: Bitmap?
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.app_widget_webcam)
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
            views.setViewVisibility(R.id.updatedAt, !live)
            views.setViewVisibility(R.id.live, live)
            views.setViewVisibility(R.id.buttonCancelLive, false)
            views.setViewVisibility(R.id.buttonRefresh, false)
            views.setViewVisibility(R.id.buttonLive, false)
            views.setViewVisibility(R.id.updatedAt, !updatedAtText.isNullOrBlank())
            views.setViewVisibility(R.id.noImageCont, frame == null)
            views.setOnClickPendingIntent(R.id.root, createLaunchAppIntent(context, webUrl))
            applyScaling(widgetId, views)
            applyDebugOptions(views, widgetId)
            return views
        }

        private fun updateLayout(appWidgetId: Int, context: Context, manager: AppWidgetManager) {
            val views = RemoteViews(context.packageName, R.layout.app_widget_webcam)
            applyScaling(appWidgetId, views)
            manager.partiallyUpdateAppWidget(appWidgetId, views)
        }

        private fun applyScaling(appWidgetId: Int, views: RemoteViews) {
            views.setViewVisibility(R.id.noImageUrl, AppWidgetPreferences.getWidgetDimensionsForWidgetId(appWidgetId).first > 190)
        }

        private fun generateImagePlaceHolder(widgetId: Int) = AppWidgetPreferences.getImageDimensionsForWidgetId(widgetId).let { size ->
            Bitmap.createBitmap(size.first.takeIf { it > 0 } ?: 1280, size.second.takeIf { it > 0 } ?: 720, Bitmap.Config.ARGB_8888)
        }
    }
}