package de.crysxd.baseui.widget.webcam

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.video.VideoSize
import de.crysxd.baseui.R
import de.crysxd.baseui.databinding.WebcamViewBinding
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.open
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min


private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 10f

class WebcamView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attributeSet, defStyle) {

    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleGestureListener())
    private var lastFocusX = 0f
    private var lastFocusY = 0f

    private val richPlayer by lazy { ExoPlayer.Builder(context).build() }
    private var playerInitialized = false

    lateinit var coroutineScope: LifecycleCoroutineScope
    private var hideLiveIndicatorJob: Job? = null

    private var transitionActive = false
    private var nativeAspectRation: Point? = null
    private var animatedMatrix = false
    var supportsToubleShooting = false
    var scaleToFill: Boolean = false
        set(value) {
            field = value
            onScaleToFillChanged()
            (state as? WebcamState.MjpegFrameReady)?.let {
                binding.mjpegSurface.imageMatrix = createMjpegMatrix(scaleToFill, it)
            }
            nativeAspectRation?.let { applyAspectRatio(it.x, it.y) }
        }

    var state: WebcamState = WebcamState.Loading
        set(value) {
            applyState(oldState = field, newState = value)
            field = value
        }

    var onSwitchWebcamClicked: () -> Unit = {}
    var onResolutionClicked: () -> Unit = {}
    var onResetConnection: () -> Unit = {}
    var onFullscreenClicked: () -> Unit = {}
    var onScaleToFillChanged: () -> Unit = {}
    var canSwitchWebcam: Boolean
        get() = binding.imageButtonSwitchCamera.isVisible
        set(value) {
            binding.imageButtonSwitchCamera.isGatedVisible = value
        }
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
        binding.mjpegSurface.scaleType = ImageView.ScaleType.MATRIX
        binding.buttonReconnect.setOnClickListener { onResetConnection() }
        binding.imageButtonFullscreen.setOnClickListener { onFullscreenClicked() }
        binding.imageButtonSwitchCamera.setOnClickListener { onSwitchWebcamClicked() }
        binding.resolutionIndicator.setOnClickListener { onResolutionClicked() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val webCamDebug = BaseInjector.get().octoPreferences().webcamBlackscreenDebug
            if (webCamDebug) {
                binding.playingState.foreground = ContextCompat.getDrawable(context, R.drawable.a_debug_1)
                binding.mjpegSurface.foreground = ContextCompat.getDrawable(context, R.drawable.a_debug_6)
                binding.errorState.foreground = ContextCompat.getDrawable(context, R.drawable.a_debug_5)
                binding.streamStalledIndicator.foreground = ContextCompat.getDrawable(context, R.drawable.a_debug_3)
                binding.reconnectingState.foreground = ContextCompat.getDrawable(context, R.drawable.a_debug_4)
                foreground = ContextCompat.getDrawable(context, R.drawable.a_debug_2)
            } else {
                binding.playingState.foreground = null
                binding.mjpegSurface.foreground = null
                binding.errorState.foreground = null
                binding.streamStalledIndicator.foreground = null
                binding.reconnectingState.foreground = null
                foreground = null
            }
        }
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
        richPlayer.pause()
        richPlayer.stop()
    }

    private fun applyState(oldState: WebcamState?, newState: WebcamState) {
        if (newState == oldState) {
            return
        }

        hideLiveIndicatorJob?.cancel()
        if (oldState == null || oldState::class != newState::class) {
            beginDelayedTransition()
            Timber.i("Moving to state ${newState::class.java.simpleName} ($this)")
            children.filter { it != binding.loadingState && it != binding.imageButtonSwitchCamera }.forEach { it.isVisible = false }
        }

        if (state !is WebcamState.MjpegFrameReady) {
            binding.mjpegSurface.setImageBitmap(null)
        }

        if (state !is WebcamState.RichStreamReady) {
            richPlayer.stop()
        }

        when (newState) {
            WebcamState.Loading -> binding.loadingState.isVisible = true
            WebcamState.NotConfigured -> {
                binding.loadingState.isVisible = false
                binding.notConfiguredState.isVisible = true
                animatedMatrix = false
            }
            WebcamState.Reconnecting -> {
                binding.loadingState.isVisible = false
                binding.reconnectingState.isVisible = true
                animatedMatrix = false
            }
            WebcamState.RichStreamDisabled -> {
                binding.loadingState.isVisible = false
                binding.errorState.isVisible = true
                binding.errorTitle.text = context.getString(R.string.rich_stream_disabled_title)
                binding.errorDescription.text = context.getString(R.string.rich_stream_disbaled_description)
                binding.buttonReconnect.text = context.getString(R.string.enable)
                animatedMatrix = false
            }
            is WebcamState.Error -> {
                binding.loadingState.isVisible = false
                binding.errorState.isVisible = true
                binding.errorTitle.text = context.getString(R.string.connection_failed)
                binding.errorDescription.text = newState.streamUrl
                binding.buttonReconnect.text = context.getString(R.string.reconnect)
                binding.buttonTroubleShoot.isVisible = supportsToubleShooting
                binding.buttonTroubleShoot.setOnClickListener {
                    UriLibrary.getWebcamTroubleshootingUri().open(findFragment<Fragment>().requireOctoActivity())
                }
                animatedMatrix = false
            }
            is WebcamState.RichStreamReady -> displayHlsStream(newState)
            is WebcamState.MjpegFrameReady -> displayMjpegFrame(newState)
        }
    }

    private fun applyAspectRatio(width: Int, height: Int) {
        val newAspectRatio = Point(width, height)
        if (newAspectRatio != nativeAspectRation) {
            nativeAspectRation = newAspectRatio

            binding.richSurface.updateLayoutParams<ConstraintLayout.LayoutParams> {
                dimensionRatio = if (scaleToFill) null else "H,$width:$height"
            }
            richPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING

            onNativeAspectRatioChanged(width, height)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scaleToFill = scaleToFill
    }

    private fun displayHlsStream(state: WebcamState.RichStreamReady) = try {
        Timber.i("Streaming ${state.uri}")
        binding.playingState.isVisible = true
        binding.richSurface.isVisible = true
        binding.mjpegSurface.isVisible = false
        binding.resolutionIndicator.isVisible = false
        usedLiveIndicator?.isVisible = false
        richPlayer.setVideoSurfaceHolder(binding.richSurface.holder)
        val mediaItem = MediaItem.fromUri(state.uri)
        val dataSourceFactory = DefaultHttpDataSource.Factory()
        state.authHeader?.let {
            dataSourceFactory.setDefaultRequestProperties(mapOf("Authorization" to it))
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        richPlayer.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        richPlayer.prepare()
        richPlayer.play()

        if (!playerInitialized) {
            playerInitialized = true

            richPlayer.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    super.onVideoSizeChanged(videoSize)
                    applyAspectRatio(videoSize.width, videoSize.height)
                }
            })
            richPlayer.addAnalyticsListener(object : AnalyticsListener {
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

                override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
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
                    if (richPlayer.isPlaying) {
                        binding.loadingState.isVisible = false
                    }
                    usedLiveIndicator?.isVisible = true
                    binding.reconnectingState.isVisible = false
                    binding.errorState.isVisible = false
                }

                override fun onLoadStarted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
                    super.onLoadStarted(eventTime, loadEventInfo, mediaLoadData)
                    Timber.v("onLoadStarted")
                    if (!richPlayer.isPlaying) {
                        binding.loadingState.isVisible = true
                    }
                }
            })
        } else {
            Timber.i("Reusing player")
        }
    } catch (e: Exception) {
        Timber.e(e)
        this.state = WebcamState.Error(state.uri.toString())
    }

    private fun invalidateMjpegFrame(frame: Bitmap) {
        binding.playingState.isGatedVisible = true
        binding.richSurface.isGatedVisible = false
        binding.mjpegSurface.isGatedVisible = true
        usedLiveIndicator?.isGatedVisible = true
        binding.loadingState.isGatedVisible = false
        binding.streamStalledIndicator.isGatedVisible = false
        binding.mjpegSurface.setImageBitmap(frame)

        // Hide live indicator if no new frame arrives within 3s
        // Show stalled indicator if no new frame arrives within 10s
        hideLiveIndicatorJob?.cancel()
        hideLiveIndicatorJob = coroutineScope.launchWhenCreated {
            val start = System.currentTimeMillis()
            delay(NOT_LIVE_IF_NO_FRAME_FOR_MS)
            beginDelayedTransition()
            usedLiveIndicator?.isVisible = false

            delay(STALLED_IF_NO_FRAME_FOR_MS - NOT_LIVE_IF_NO_FRAME_FOR_MS)
            beginDelayedTransition()
            binding.streamStalledIndicator.isVisible = true
            do {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)
                binding.streamStalledIndicatorDetail.text = context.getString(R.string.no_frames_since_xs, seconds)
                delay(1000)
            } while (isActive)
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

    private fun displayMjpegFrame(newState: WebcamState.MjpegFrameReady) {
        // Do not update frame if a transition is active
        if (transitionActive) {
            Timber.i("Drop frame, transition active")
            return
        }

        // Some important view state changed? Animate!
        if (binding.loadingState.isVisible || binding.streamStalledIndicator.isVisible || usedLiveIndicator?.isVisible == false) {
            beginDelayedTransition()
        }

        // Update image matrix and set the animated flag. As it's now initialized, we can animate changes
        // TODO This also needs to factor in view size changes!
//        val oldState = state as? WebcamState.MjpegFrameReady
//        if (newState.flipH != oldState?.flipH || newState.flipV != oldState.flipV || newState.rotate90 != oldState.rotate90 || oldState.frame.width != newState.frame.width || oldState.frame.height != newState.frame.height) {
        binding.mjpegSurface.imageMatrix = createMjpegMatrix(scaleToFill, newState)
        animatedMatrix = true
//        }

        val size = min(newState.frame.width, newState.frame.height)
        @SuppressLint("SetTextI18n")
        binding.resolutionIndicator.text = "${size}p"
        applyAspectRatio(newState.frame.width, newState.frame.height)
        binding.resolutionIndicator.isVisible = BaseInjector.get().octoPreferences().isShowWebcamResolution
        invalidateMjpegFrame(newState.frame)
    }

    private fun beginDelayedTransition() {
//        Suspect to cause #922
//        TransitionManager.endTransitions(this)
//        TransitionManager.beginDelayedTransition(this, TransitionSet().also {
//            it.addTransition(Fade())
//            if (animatedMatrix) {
//                it.addTransition(ChangeImageTransform())
//            }
//            it.addListener(object : Transition.TransitionListener {
//                override fun onTransitionStart(transition: Transition) {
//                    transitionActive = true
//                }
//
//                override fun onTransitionEnd(transition: Transition) {
//                    transitionActive = false
//                }
//
//                override fun onTransitionCancel(transition: Transition) {
//                    transitionActive = false
//                }
//
//                override fun onTransitionPause(transition: Transition) = Unit
//
//                override fun onTransitionResume(transition: Transition) = Unit
//
//            })
//        })
    }

    private fun zoom(zoomChange: Float, focusX: Float, focusY: Float) {
        fun View.zoomInternal(zoomChange: Float, focusX: Float, focusY: Float) {
            val newZoom = (scaleX * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
            scaleX = newZoom
            scaleY = newZoom
            val focusShiftX = focusX - lastFocusX
            val focusShiftY = focusY - lastFocusY
            val centerOffsetX = ((width / 2) - translationX) - focusX
            val centerOffsetY = ((height / 2) - translationY) - focusY
            translationX += focusShiftX + centerOffsetX
            translationY += focusShiftY + centerOffsetY
        }

        binding.richSurface.zoomInternal(zoomChange, focusX, focusY)
        binding.mjpegSurface.zoomInternal(zoomChange, focusX, focusY)
        lastFocusX = focusX
        lastFocusY = focusY

        limitScrollRange()
    }

    private fun createMjpegMatrix(fit: Boolean, state: WebcamState.MjpegFrameReady): Matrix {
        val matrix = Matrix()

        // Apply rotation (around center)
        val (frameWidth, frameHeight) = if (state.rotate90) {
            matrix.postRotate(-90f, state.frame.width / 2f, state.frame.height / 2f)
            state.frame.height to state.frame.width
        } else {
            state.frame.width to state.frame.height
        }

        // Apply flips
        if (state.flipH || state.flipV) {
            matrix.postScale(
                if (state.flipH) -1f else 1f,
                if (state.flipV) -1f else 1f,
                state.frame.width / 2f,
                state.frame.height / 2f
            )
        }

        // Apply scale to fit or fill view
        val scaleY = height / frameHeight.toFloat()
        val scaleX = width / frameWidth.toFloat()
        val scale = if (fit) {
            max(scaleX, scaleY)
        } else {
            min(scaleX, scaleY)
        }
        matrix.postScale(scale, scale)

        // Center in view
        val dx = (width - (state.frame.width * scale)) / 2
        val dy = (height - (state.frame.height * scale)) / 2
        matrix.postTranslate(dx, dy)
        Timber.d("Creating matrix with flipH=${state.flipH} flipV=${state.flipV} scale=$scale, dx=$dx, dy=$dy")
        return matrix
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

        binding.richSurface.limitScrollRangeInternal()
        binding.mjpegSurface.limitScrollRangeInternal()
    }

    sealed class WebcamState {
        object Loading : WebcamState()
        object Reconnecting : WebcamState()
        object NotConfigured : WebcamState()
        object RichStreamDisabled : WebcamState()
        data class Error(val streamUrl: String?) : WebcamState()
        data class RichStreamReady(val uri: Uri, val authHeader: String?) : WebcamState()
        data class MjpegFrameReady(
            val frame: Bitmap,
            val flipH: Boolean,
            val flipV: Boolean,
            val rotate90: Boolean,
        ) : WebcamState()
    }

    inner class ScaleGestureListener : ScaleGestureDetector.OnScaleGestureListener {
        private var hintShown = false
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Check if we increase or decrease zoom
            zoom(focusX = detector.focusX, focusY = detector.focusY, zoomChange = detector.scaleFactor)

            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            lastFocusX = detector.focusX
            lastFocusY = detector.focusY
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            hintShown = false
        }
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onShowPress(e: MotionEvent) = Unit

        override fun onSingleTapUp(e: MotionEvent) = false

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            binding.richSurface.translationX -= distanceX
            binding.richSurface.translationY -= distanceY
            binding.mjpegSurface.translationX -= distanceX
            binding.mjpegSurface.translationY -= distanceY
            limitScrollRange()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            beginDelayedTransition()
            if (binding.mjpegSurface.scaleX > 1) {
                zoom(zoomChange = 1f, focusX = 0f, focusY = 0f)
            } else {
                scaleToFill = !scaleToFill
            }
            return true
        }

        override fun onLongPress(e: MotionEvent?) = Unit

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float) = false

    }
}