package de.crysxd.octoapp.base.ui.webcam

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.transition.TransitionManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.video.VideoListener
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.ui.widget.webcam.NOT_LIVE_IF_NO_FRAME_FOR_MS
import de.crysxd.octoapp.base.ui.widget.webcam.STALLED_IF_NO_FRAME_FOR_MS
import kotlinx.android.synthetic.main.view_webcam.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.TimeUnit


private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 10f
private const val ZOOM_SPEED = 0.2f
private const val DOUBLE_TAP_ZOOM = 5f


class WebcamView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attributeSet, defStyle) {

    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleGestureListener())
    private var scrollOffset = PointF(0f, 0f)
    private var zoom
        get() = hlsSurface.scaleX
        set(value) {
            hlsSurface.scaleX = value
            hlsSurface.scaleY = value
            mjpegSurface.scaleX = value
            mjpegSurface.scaleY = value
        }

    private val hlsPlayer by lazy { SimpleExoPlayer.Builder(context).build() }
    private var playerInitialized = false

    lateinit var coroutineScope: LifecycleCoroutineScope
    private var hideLiveIndicatorJob: Job? = null


    var state: WebcamState = WebcamState.Loading
        set(value) {
            field = value
            applyState()
        }

    var onResetConnection: () -> Unit = {}
    var onFullscreenClicked: () -> Unit = {}
    var fullscreenIconResource: Int
        get() = 0
        set(value) {
            imageButtonFullscreen.setImageResource(value)
        }
    var onNativeAspectRatioChanged: (width: Int, height: Int) -> Unit = { _, _ -> }
    var usedLiveIndicator: View? = null
        set(value) {
            field = value
            if (value != liveIndicator) {
                liveIndicator.isVisible = false
            }
        }

    init {
        View.inflate(context, R.layout.view_webcam, this)
        applyState()
        usedLiveIndicator = liveIndicator
        buttonReconnect.setOnClickListener { onResetConnection() }
        imageButtonFullscreen.setOnClickListener { onFullscreenClicked() }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            gestureDetector.onTouchEvent(event)
        }
        return true
    }

    private fun applyState() {
        beginDelayedTransition()
        children.forEach { it.isVisible = false }
        when (val localState = state) {
            WebcamState.Loading -> loadingState.show()
            WebcamState.NotConfigured -> notConfiguredState.isVisible = true
            WebcamState.Reconnecting -> reconnectingState.isVisible = true
            is WebcamState.Error -> errorState.isVisible = true
            is WebcamState.HlsStreamReady -> displayHlsStream(localState)
            is WebcamState.MjpegFrameReady -> displayMjpegFrame(localState)
        }
    }

    private fun displayHlsStream(state: WebcamState.HlsStreamReady) {
        playingState.isVisible = true
        hlsSurface.isVisible = true
        mjpegSurface.isVisible = false
        usedLiveIndicator?.isVisible = true
        loadingState.hide()
        hlsPlayer.setVideoSurfaceHolder(hlsSurface.holder)
        hlsPlayer.setMediaItem(MediaItem.fromUri(state.uri))
        hlsPlayer.videoScalingMode = Renderer.VIDEO_SCALING_MODE_DEFAULT
        hlsPlayer.prepare()
        hlsPlayer.play()

        if (!playerInitialized) {
            playerInitialized = true

            hlsPlayer.addVideoListener(object : VideoListener {
                override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                    super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                    onNativeAspectRatioChanged(width, height)
                    hlsSurface.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        this.dimensionRatio = "H,$width:$height"
                    }
                }
            })

            hlsPlayer.addAnalyticsListener(object : AnalyticsListener {
                override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
                    super.onIsPlayingChanged(eventTime, isPlaying)
                    Timber.v("onIsPlayingChanged: $isPlaying")
                    if (isPlaying) {
                        errorState.isVisible = false
                        reconnectingState.isVisible = false
                        loadingState.hide()
                        usedLiveIndicator?.isVisible = true
                    }
                }

                override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: ExoPlaybackException) {
                    super.onPlayerError(eventTime, error)
                    Timber.v("onPlayerError")
                    loadingState.hide()
                    errorState.isVisible = true
                    reconnectingState.isVisible = false
                    usedLiveIndicator?.isVisible = false
                    errorDescription.text = context.getString(R.string.error_video_playback, state.uri, error.message)
                }

                override fun onLoadCompleted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
                    super.onLoadCompleted(eventTime, loadEventInfo, mediaLoadData)
                    Timber.v("onLoadCompleted")
                    loadingState.hide()
                    usedLiveIndicator?.isVisible = true
                    reconnectingState.isVisible = false
                    errorState.isVisible = false
                }

                override fun onLoadStarted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
                    super.onLoadStarted(eventTime, loadEventInfo, mediaLoadData)
                    Timber.v("onLoadStarted")
                    if (!hlsPlayer.isPlaying) {
                        loadingState.show()
                    }
                }
            })
        }
    }

    private fun displayMjpegFrame(state: WebcamState.MjpegFrameReady) {
        playingState.isVisible = true
        hlsSurface.isVisible = false
        mjpegSurface.isVisible = true
        usedLiveIndicator?.isVisible = true
        loadingState.hide()
        mjpegSurface.setImageBitmap(state.frame)

        onNativeAspectRatioChanged(state.frame.width, state.frame.height)

        // Hide live indicator if no new frame arrives within 3s
        // Show stalled indicator if no new frame arrives within 10s
        hideLiveIndicatorJob?.cancel()
        hideLiveIndicatorJob = coroutineScope.launchWhenCreated {
            delay(NOT_LIVE_IF_NO_FRAME_FOR_MS)
            beginDelayedTransition()
            usedLiveIndicator?.isVisible = false

            delay(STALLED_IF_NO_FRAME_FOR_MS - NOT_LIVE_IF_NO_FRAME_FOR_MS)
            beginDelayedTransition()
            val seconds = TimeUnit.MILLISECONDS.toSeconds(STALLED_IF_NO_FRAME_FOR_MS)
            streamStalledIndicatorDetail.text = context.getString(R.string.no_frames_since_xs, seconds)
            streamStalledIndicator.isVisible = true
        }
    }

    private fun beginDelayedTransition() = TransitionManager.beginDelayedTransition(this, InstantAutoTransition())

    private fun limitScrollRange() {
        val scaledSurfaceWidth = hlsSurface.width * zoom
        val scaledSurfaceHeight = hlsSurface.height * zoom
        val maxX = ((scaledSurfaceWidth - width) * 0.5f).coerceAtLeast(0f)
        val minX = -maxX
        val maxY = ((scaledSurfaceHeight - height) * 0.5f).coerceAtLeast(0f)
        val minY = -maxY
        Timber.i("zoom=$zoom x=${hlsSurface.translationX} y=${hlsSurface.translationY} minY=$minY maxY=$maxY")

        if (hlsSurface.translationX > maxX) {
            hlsSurface.translationX = maxX
        } else if (hlsSurface.translationX < minX) {
            hlsSurface.translationX = minX
        }

        if (hlsSurface.translationY > maxY) {
            hlsSurface.translationY = maxY
        } else if (hlsSurface.translationY < minY) {
            hlsSurface.translationY = minY
        }
    }

    sealed class WebcamState {
        object Loading : WebcamState()
        object Reconnecting : WebcamState()
        object NotConfigured : WebcamState()
        object Error : WebcamState()
        data class HlsStreamReady(val uri: Uri) : WebcamState()
        data class MjpegFrameReady(val frame: Bitmap) : WebcamState()
    }

    inner class ScaleGestureListener : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Check if we increase or decrease zoom
//            val scaleDirection = if (detector.previousSpan > detector.currentSpan) -1f else 1f
//            val zoomChange = detector.scaleFactor * ZOOM_SPEED * scaleDirection
//            val newZoom = (zoom + zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
//            zoom(detector.focusX, detector.focusY, newZoom)
//            invalidate()
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector) = true

        override fun onScaleEnd(detector: ScaleGestureDetector) = Unit
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onShowPress(e: MotionEvent) = Unit

        override fun onSingleTapUp(e: MotionEvent) = false

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
//            renderParams?.let {
//                scrollOffset.x -= distanceX
//                scrollOffset.y -= distanceY
//                invalidate()
//            }

            hlsSurface.translationX -= distanceX
            hlsSurface.translationY -= distanceY
            mjpegSurface.translationX = hlsSurface.translationX
            mjpegSurface.translationY = hlsSurface.translationY
            limitScrollRange()

            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val newZoom = if (zoom > 1) {
                1f
            } else {
                DOUBLE_TAP_ZOOM
            }

            ObjectAnimator.ofFloat(this@WebcamView, "zoom", zoom, newZoom).also {
                it.addUpdateListener { limitScrollRange() }
            }.start()
            return true
        }

        override fun onLongPress(e: MotionEvent?) = Unit

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float) = false

    }
}