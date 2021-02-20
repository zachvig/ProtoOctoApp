package de.crysxd.octoapp.base.ui.widget.webcam

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.findNavController
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.video.VideoListener
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.WebcamViewBinding
import de.crysxd.octoapp.base.ui.common.troubleshoot.TroubleShootingFragmentArgs
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
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
    var supportsToubleShooting = false
    var scaleToFill: Boolean = false
        set(value) {
            field = value
            onScaleToFillChanged()
            binding.mjpegSurface.scaleType = if (scaleToFill) {
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
            binding.imageButtonFullscreen.setImageResource(value)
        }
    var onNativeAspectRatioChanged: (width: Int, height: Int) -> Unit = { _, _ -> }
    var usedLiveIndicator: View? = null
        set(value) {
            field = value
            if (value != binding.liveIndicator) {
                binding.liveIndicator.isVisible = false
            }
        }

    private val binding = WebcamViewBinding.inflate(LayoutInflater.from(context), this)

    init {
        applyState(null, state)
        usedLiveIndicator = binding.liveIndicator
        binding.buttonReconnect.setOnClickListener { onResetConnection() }
        binding.imageButtonFullscreen.setOnClickListener { onFullscreenClicked() }
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
        if (newState == oldState) {
            return
        }

        hideLiveIndicatorJob?.cancel()
        if (oldState == null || oldState::class != newState::class) {
            beginDelayedTransition()
            Timber.i("Moving to state ${newState::class.java.simpleName} ($this)")
            children.filter { it != binding.loadingState }.forEach { it.isVisible = false }
        }

        if (state !is WebcamState.MjpegFrameReady) {
            binding.mjpegSurface.setImageBitmap(null)
        }


        when (newState) {
            WebcamState.Loading -> binding.loadingState.isVisible = true
            WebcamState.NotConfigured -> {
                binding.loadingState.isVisible = false
                binding.notConfiguredState.isVisible = true
            }
            WebcamState.Reconnecting -> {
                binding.loadingState.isVisible = false
                binding.reconnectingState.isVisible = true
            }
            WebcamState.HlsStreamDisabled -> {
                binding.loadingState.isVisible = false
                binding.errorState.isVisible = true
                binding.errorTitle.text = context.getString(R.string.hls_stream_disabled_title)
                binding.errorDescription.text = context.getString(R.string.hls_stream_disbaled_description)
                binding.buttonReconnect.text = context.getString(R.string.enable)
            }
            is WebcamState.Error -> {
                binding.loadingState.isVisible = false
                binding.errorState.isVisible = true
                binding.errorTitle.text = context.getString(R.string.connection_failed)
                binding.errorDescription.text = newState.streamUrl
                binding.buttonReconnect.text = context.getString(R.string.reconnect)
                binding.buttonTroubleShoot.isVisible = supportsToubleShooting
                binding.buttonTroubleShoot.setOnClickListener {
                    findNavController().navigate(
                        R.id.action_trouble_shoot,
                        TroubleShootingFragmentArgs(Uri.parse(newState.streamUrl), null).toBundle()
                    )
                }
            }
            is WebcamState.HlsStreamReady -> displayHlsStream(newState)
            is WebcamState.MjpegFrameReady -> displayMjpegFrame(newState)
        }
    }

    private fun applyAspectRatio(width: Int, height: Int) {
        nativeAspectRation = Point(width, height)

        binding.hlsSurface.updateLayoutParams<ConstraintLayout.LayoutParams> {
            dimensionRatio = if (scaleToFill) null else "H,$width:$height"
        }
        hlsPlayer.videoScalingMode = Renderer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING

        onNativeAspectRatioChanged(width, height)
    }

    private fun displayHlsStream(state: WebcamState.HlsStreamReady) {
        binding.playingState.isVisible = true
        binding.hlsSurface.isVisible = true
        binding.mjpegSurface.isVisible = false
        usedLiveIndicator?.isVisible = false
        hlsPlayer.setVideoSurfaceHolder(binding.hlsSurface.holder)
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
                        binding.errorState.isVisible = false
                        binding.reconnectingState.isVisible = false
                        binding.loadingState.isVisible = false
                        usedLiveIndicator?.isVisible = true
                    }
                }

                override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: ExoPlaybackException) {
                    super.onPlayerError(eventTime, error)
                    Timber.v("onPlayerError")
                    binding.loadingState.isVisible = false
                    binding.errorState.isVisible = true
                    binding.reconnectingState.isVisible = false
                    usedLiveIndicator?.isVisible = false
                    binding.errorDescription.text = context.getString(R.string.error_video_playback, state.uri, error.message)
                }

                override fun onLoadCompleted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
                    super.onLoadCompleted(eventTime, loadEventInfo, mediaLoadData)
                    Timber.v("onLoadCompleted")
                    if (hlsPlayer.isPlaying) {
                        binding.loadingState.isVisible = false
                    }
                    usedLiveIndicator?.isVisible = true
                    binding.reconnectingState.isVisible = false
                    binding.errorState.isVisible = false
                }

                override fun onLoadStarted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
                    super.onLoadStarted(eventTime, loadEventInfo, mediaLoadData)
                    Timber.v("onLoadStarted")
                    if (!hlsPlayer.isPlaying) {
                        binding.loadingState.isVisible = true
                    }
                }
            })
        }
    }

    fun invalidateMjpegFrame() {
        binding.playingState.isGatedVisible = true
        binding.hlsSurface.isGatedVisible = false
        binding.mjpegSurface.isGatedVisible = true
        usedLiveIndicator?.isGatedVisible = true
        binding.loadingState.isGatedVisible = false
        binding.streamStalledIndicator.isGatedVisible = false

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
            binding.streamStalledIndicatorDetail.text = context.getString(R.string.no_frames_since_xs, seconds)
            binding.streamStalledIndicator.isVisible = true
        }

        invalidate()
    }

    private var View.isGatedVisible
        get() = isVisible
        set(value) {
            if (isVisible != value) {
                isVisible = value
            }
        }

    private fun displayMjpegFrame(state: WebcamState.MjpegFrameReady) {
        // Do not update frame if a transition is active
        if (transitionActive) {
            Timber.i("Drop frame, transition active")
            return
        }

        // Some important view state changed? Animate!
        if (binding.loadingState.isVisible || binding.streamStalledIndicator.isVisible || usedLiveIndicator?.isVisible == false) {
            beginDelayedTransition()
        }

        binding.mjpegSurface.setImageBitmap(state.frame)
        applyAspectRatio(state.frame.width, state.frame.height)

        invalidateMjpegFrame()
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

        binding.hlsSurface.zoomInternal(newZoom, focusX, focusY)
        binding.mjpegSurface.zoomInternal(newZoom, focusX, focusY)

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

        binding.hlsSurface.limitScrollRangeInternal()
        binding.mjpegSurface.limitScrollRangeInternal()
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
        private var hintShown = false
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (!scaleToFill) {
                // Check if we increase or decrease zoom
                val scaleDirection = if (detector.previousSpan > detector.currentSpan) -1f else 1f
                val zoomChange = detector.scaleFactor * ZOOM_SPEED * scaleDirection
                val newZoom = (binding.hlsSurface.scaleX + zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
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
            binding.hlsSurface.translationX -= distanceX
            binding.hlsSurface.translationY -= distanceY
            binding.mjpegSurface.translationX -= distanceX
            binding.mjpegSurface.translationY -= distanceY
            limitScrollRange()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            beginDelayedTransition()
            if (binding.mjpegSurface.scaleX > 1) {
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