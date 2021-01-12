package de.crysxd.octoapp.base.ui.widget.webcam

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.video.VideoListener
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import kotlinx.android.synthetic.main.view_webcam.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.TimeUnit


private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 10f
private const val ZOOM_SPEED = 0.2f

class WebcamView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attributeSet, defStyle) {

    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleGestureListener())

    private val hlsPlayer by lazy { SimpleExoPlayer.Builder(context).build() }
    private var playerInitialized = false

    lateinit var coroutineScope: LifecycleCoroutineScope
    private var hideLiveIndicatorJob: Job? = null

    private var transitionActive = false
    private var nativeAspectRation: Point? = null
    var scaleToFill: Boolean = false
        set(value) {
            field = value
            onScaleToFillChanged()
            mjpegSurface.scaleType = if (scaleToFill) {
                ImageView.ScaleType.CENTER_CROP
            } else {
                ImageView.ScaleType.FIT_CENTER
            }
            nativeAspectRation?.let { applyAspectRatio(it.x, it.y) }
        }

    var state: WebcamState = WebcamState.Loading
        set(value) {
            applyState(oldState = field, newState = value)
            field = value
        }

    var onResetConnection: () -> Unit = {}
    var onFullscreenClicked: () -> Unit = {}
    var onScaleToFillChanged: () -> Unit = {}
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
        applyState(null, state)
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

    fun onPause() {
        Timber.i("Stopping stream")
        hlsPlayer.pause()
        hlsPlayer.stop()
    }

    private fun applyState(oldState: WebcamState?, newState: WebcamState) {
        hideLiveIndicatorJob?.cancel()
        if (oldState == null || oldState::class != newState::class) {
            beginDelayedTransition()
            Timber.i("Moving to state ${newState::class.java.simpleName} ($this)")
            children.filter { it != loadingState }.forEach { it.isVisible = false }
        }

        if (state !is WebcamState.MjpegFrameReady) {
            mjpegSurface.setImageBitmap(null)
        }

        when (newState) {
            WebcamState.Loading -> loadingState.isVisible = true
            WebcamState.NotConfigured -> {
                loadingState.isVisible = false
                notConfiguredState.isVisible = true
            }
            WebcamState.Reconnecting -> {
                loadingState.isVisible = false
                reconnectingState.isVisible = true
            }
            WebcamState.HlsStreamDisabled -> {
                loadingState.isVisible = false
                errorState.isVisible = true
                errorTitle.text = context.getString(R.string.hls_stream_disabled_title)
                errorDescription.text = context.getString(R.string.hls_stream_disbaled_description)
                buttonReconnect.text = context.getString(R.string.enable)
            }
            is WebcamState.Error -> {
                loadingState.isVisible = false
                errorState.isVisible = true
                errorTitle.text = context.getString(R.string.connection_failed)
                errorDescription.text = newState.streamUrl
                buttonReconnect.text = context.getString(R.string.reconnect)
            }
            is WebcamState.HlsStreamReady -> displayHlsStream(newState)
            is WebcamState.MjpegFrameReady -> displayMjpegFrame(newState)
        }
    }

    private fun applyAspectRatio(width: Int, height: Int) {
        nativeAspectRation = Point(width, height)

        hlsSurface.updateLayoutParams<ConstraintLayout.LayoutParams> {
            this.dimensionRatio = if (scaleToFill) null else "H,$width:$height"
        }
        hlsPlayer.videoScalingMode = Renderer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING

        onNativeAspectRatioChanged(width, height)
    }

    private fun displayHlsStream(state: WebcamState.HlsStreamReady) {
        playingState.isVisible = true
        hlsSurface.isVisible = true
        mjpegSurface.isVisible = false
        usedLiveIndicator?.isVisible = false
        hlsPlayer.setVideoSurfaceHolder(hlsSurface.holder)
        hlsPlayer.setMediaItem(MediaItem.fromUri(state.uri))
        hlsPlayer.prepare()
        hlsPlayer.play()

        if (!playerInitialized) {
            playerInitialized = true

            hlsPlayer.addVideoListener(object : VideoListener {
                override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                    super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                    applyAspectRatio(width, height)
                }
            })

            hlsPlayer.addAnalyticsListener(object : AnalyticsListener {
                override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
                    super.onIsPlayingChanged(eventTime, isPlaying)
                    Timber.v("onIsPlayingChanged: $isPlaying")
                    usedLiveIndicator?.isVisible = isPlaying
                    if (isPlaying) {
                        errorState.isVisible = false
                        reconnectingState.isVisible = false
                        loadingState.isVisible = false
                        usedLiveIndicator?.isVisible = true
                    }
                }

                override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: ExoPlaybackException) {
                    super.onPlayerError(eventTime, error)
                    Timber.v("onPlayerError")
                    loadingState.isVisible = false
                    errorState.isVisible = true
                    reconnectingState.isVisible = false
                    usedLiveIndicator?.isVisible = false
                    errorDescription.text = context.getString(R.string.error_video_playback, state.uri, error.message)
                }

                override fun onLoadCompleted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
                    super.onLoadCompleted(eventTime, loadEventInfo, mediaLoadData)
                    Timber.v("onLoadCompleted")
                    if (hlsPlayer.isPlaying) {
                        loadingState.isVisible = false
                    }
                    usedLiveIndicator?.isVisible = true
                    reconnectingState.isVisible = false
                    errorState.isVisible = false
                }

                override fun onLoadStarted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
                    super.onLoadStarted(eventTime, loadEventInfo, mediaLoadData)
                    Timber.v("onLoadStarted")
                    if (!hlsPlayer.isPlaying) {
                        loadingState.isVisible = true
                    }
                }
            })
        }
    }

    private fun displayMjpegFrame(state: WebcamState.MjpegFrameReady) {
        // Do not update frame if a transition is active
        if (transitionActive) {
            Timber.i("Drop frame, transition active")
            return
        }

        // Some important view state changed? Animate!
        if (loadingState.isVisible || streamStalledIndicator.isVisible || usedLiveIndicator?.isVisible == false) {
            beginDelayedTransition()
        }

        playingState.isVisible = true
        hlsSurface.isVisible = false
        mjpegSurface.isVisible = true
        usedLiveIndicator?.isVisible = true
        loadingState.isVisible = false
        streamStalledIndicator.isVisible = false

        mjpegSurface.setImageBitmap(state.frame)
        applyAspectRatio(state.frame.width, state.frame.height)

        // Hide live indicator if no new frame arrives within 3s
        // Show stalled indicator if no new frame arrives within 10s
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

    private fun beginDelayedTransition() = TransitionManager.beginDelayedTransition(this, InstantAutoTransition().also {
        it.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) {
                transitionActive = true
            }

            override fun onTransitionEnd(transition: Transition) {
                transitionActive = false
            }

            override fun onTransitionCancel(transition: Transition) {
                transitionActive = false
            }

            override fun onTransitionPause(transition: Transition) = Unit

            override fun onTransitionResume(transition: Transition) = Unit

        })
    })

    private fun zoom(newZoom: Float, focusX: Float, focusY: Float) {
        fun View.zoomInternal(newZoom: Float, focusX: Float, focusY: Float) {
            val targetOnVideoX = (translationX + focusX) / scaleX
            val targetOnVideoY = (translationX + focusY) / scaleY
            val targetOnVideoXAfter = (translationX + focusX) / newZoom
            val targetOnVideoYAfter = (translationX + focusY) / newZoom
            val additionalScrollX = targetOnVideoX - targetOnVideoXAfter
            val additionalScrollY = targetOnVideoY - targetOnVideoYAfter

            scaleX = newZoom
            scaleY = newZoom
            translationX += additionalScrollX
            translationY += additionalScrollY
        }

        hlsSurface.zoomInternal(newZoom, focusX, focusY)
        mjpegSurface.zoomInternal(newZoom, focusX, focusY)

        limitScrollRange()
    }

    private fun limitScrollRange() {
        fun View.limitScrollRangeInternal() {
            // height is not reliable for MJPEG, so we calculate it based on the reliable width
            val scaledSurfaceWidth = width * scaleX
            val scaledSurfaceHeight = nativeAspectRation?.let {
                scaledSurfaceWidth * (it.y / it.x.toFloat())
            } ?: height * scaleY
            val maxX = ((scaledSurfaceWidth - this@WebcamView.width) * 0.5f).coerceAtLeast(0f)
            val minX = -maxX
            val maxY = ((scaledSurfaceHeight - this@WebcamView.height) * 0.5f).coerceAtLeast(0f)
            val minY = -maxY

            if (translationX > maxX) {
                translationX = maxX
            } else if (translationX < minX) {
                translationX = minX
            }

            if (translationY > maxY) {
                translationY = maxY
            } else if (translationY < minY) {
                translationY = minY
            }
        }

        hlsSurface.limitScrollRangeInternal()
        mjpegSurface.limitScrollRangeInternal()
    }

    sealed class WebcamState {
        object Loading : WebcamState()
        object Reconnecting : WebcamState()
        object NotConfigured : WebcamState()
        object HlsStreamDisabled : WebcamState()
        data class Error(val streamUrl: String?) : WebcamState()
        data class HlsStreamReady(val uri: Uri) : WebcamState()
        data class MjpegFrameReady(val frame: Bitmap) : WebcamState()
    }

    inner class ScaleGestureListener : ScaleGestureDetector.OnScaleGestureListener {
        var hintShown = false
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (!scaleToFill) {
                // Check if we increase or decrease zoom
                val scaleDirection = if (detector.previousSpan > detector.currentSpan) -1f else 1f
                val zoomChange = detector.scaleFactor * ZOOM_SPEED * scaleDirection
                val newZoom = (hlsSurface.scaleX + zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
                zoom(focusX = detector.focusX, focusY = detector.focusY, newZoom = newZoom)
            } else if (!hintShown) {
                hintShown = true
                Toast.makeText(context, context.getString(R.string.cant_zoom_in_fit_mode), Toast.LENGTH_SHORT).show()
            }

            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector) = true

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            hintShown = false
        }
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onShowPress(e: MotionEvent) = Unit

        override fun onSingleTapUp(e: MotionEvent) = false

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            hlsSurface.translationX -= distanceX
            hlsSurface.translationY -= distanceY
            mjpegSurface.translationX -= distanceX
            mjpegSurface.translationY -= distanceY
            limitScrollRange()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            beginDelayedTransition()
            if (mjpegSurface.scaleX > 1) {
                zoom(newZoom = 1f, focusX = 0f, focusY = 0f)
            } else {
                scaleToFill = !scaleToFill
            }
            return true
        }

        override fun onLongPress(e: MotionEvent?) = Unit

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float) = false

    }
}