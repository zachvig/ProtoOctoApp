package de.crysxd.baseui.widget.webcam

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
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
import kotlin.math.min

class WebcamView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attributeSet, defStyle) {

    companion object {
        const val LIVE_DELAY_THRESHOLD_MS = 3_000L
        const val STALLED_THRESHOLD_MS = 5_000L
    }

    private val binding = WebcamViewBinding.inflate(LayoutInflater.from(context), this)
    lateinit var coroutineScope: LifecycleCoroutineScope
    private var liveIndicatorJob: Job? = null
    private var grabBitmap: () -> Bitmap? = { null }

    private val richPlayer by lazy { ExoPlayer.Builder(context).build() }
    var lastRichPlayerListener: Player.Listener? = null
    var lastRichPlayerAnalyticsListener: AnalyticsListener? = null
    var lastNativeWidth: Int? = null
    var lastNativeHeight: Int? = null

    var supportsTroubleShooting = false
    var scaleToFill: Boolean
        get() = binding.matrixView.scaleToFill
        set(value) {
            binding.matrixView.scaleToFill = value
        }

    var state: WebcamState = WebcamState.Loading
        set(value) {
            applyState(oldState = field, newState = value)
            field = value
        }

    var onSwitchWebcamClicked: () -> Unit = {}
    var onShareImageClicked: (suspend () -> Bitmap?) -> Unit = {}
    var onResolutionClicked: () -> Unit = {}
    var onResetConnection: () -> Unit = {}
    var onFullscreenClicked: () -> Unit = {}
    var onNativeAspectRatioChanged: (ratio: String, width: Int, height: Int) -> Unit = { _, _, _ -> }
    var onScaleToFillChanged
        get() = binding.matrixView.onScaleToFillChanged
        set(value) {
            binding.matrixView.onScaleToFillChanged = value
        }

    var canSwitchWebcam: Boolean
        get() = binding.imageButtonSwitchCamera.isVisible
        set(value) {
            binding.imageButtonSwitchCamera.isVisible = value
            binding.imageButtonSwitchCameraInPlay.isVisible = value
        }
    var fullscreenIconResource: Int
        get() = 0
        set(value) {
            binding.imageButtonFullscreen.setImageResource(value)
        }
    var usedLiveIndicator: TextView? = null
        set(value) {
            field = value
            if (value != binding.liveIndicator) {
                binding.liveIndicator.isVisible = false
            }
        }

    init {
        setWillNotDraw(false)
        applyState(null, state)
        usedLiveIndicator = binding.liveIndicator
        binding.mjpegSurface.scaleType = ImageView.ScaleType.MATRIX
        binding.buttonReconnect.setOnClickListener { onResetConnection() }
        binding.imageButtonFullscreen.setOnClickListener { onFullscreenClicked() }
        binding.imageButtonSwitchCamera.setOnClickListener { onSwitchWebcamClicked() }
        binding.resolutionIndicator.setOnClickListener { onResolutionClicked() }
        binding.imageButtonShare.setOnClickListener { onShareImageClicked(captureBitmap()) }
    }

    @SuppressLint("SetTextI18n")
    private fun dispatchNativeContentDimensionChanged(width: Int, height: Int, rotate90: Boolean) {
        val w = if (rotate90) height else width
        val h = if (rotate90) width else height
        val ratio = "$w:$h"
        if (w != lastNativeWidth || h != lastNativeHeight) {
            // Dispatch 
            Timber.i("Dispatching native aspect ratio: $ratio")
            lastNativeHeight = h
            lastNativeWidth = w
            onNativeAspectRatioChanged(ratio, w, h)


            val size = min(w, h)
            binding.resolutionIndicator.text = "${size}p"
            binding.resolutionIndicator.isVisible = BaseInjector.get().octoPreferences().isShowWebcamResolution
        }
    }

    fun onPause() {
        Timber.i("Stopping stream")
        richPlayer.pause()
        richPlayer.stop()
    }

    private fun applyState(oldState: WebcamState?, newState: WebcamState) {
        if (newState == oldState && newState !is WebcamState.RichStreamReady) {
            return
        }

        liveIndicatorJob?.cancel()
        if (oldState == null || oldState::class != newState::class) {
            Timber.i("Moving to state ${newState::class.java.simpleName} ($this)")
            children.filter { it != binding.loadingState && it != binding.imageButtonSwitchCamera }.forEach { it.isVisible = false }
            lastNativeWidth = null
            lastNativeHeight = null
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
            }
            WebcamState.Reconnecting -> {
                binding.loadingState.isVisible = false
                binding.reconnectingState.isVisible = true
            }
            WebcamState.RichStreamDisabled -> {
                binding.loadingState.isVisible = false
                binding.errorState.isVisible = true
                binding.errorTitle.text = context.getString(R.string.rich_stream_disabled_title)
                binding.errorDescription.text = context.getString(R.string.rich_stream_disbaled_description)
                binding.buttonReconnect.text = context.getString(R.string.enable)
            }
            is WebcamState.Error -> {
                binding.loadingState.isVisible = false
                binding.errorState.isVisible = true
                binding.errorTitle.text = context.getString(R.string.connection_failed)
                binding.errorDescription.text = newState.streamUrl
                binding.buttonReconnect.text = context.getString(R.string.reconnect)
                binding.buttonTroubleShoot.isVisible = supportsTroubleShooting
                binding.buttonTroubleShoot.setOnClickListener {
                    UriLibrary.getWebcamTroubleshootingUri().open(findFragment<Fragment>().requireOctoActivity())
                }
            }
            is WebcamState.RichStreamReady -> displayHlsStream(newState)
            is WebcamState.MjpegFrameReady -> displayMjpegFrame(newState)
        }
    }

    private fun displayHlsStream(state: WebcamState.RichStreamReady) = try {
        Timber.i("Streaming ${state.uri}")
        binding.playingState.isVisible = true
        binding.richSurface.isVisible = true
        binding.mjpegSurface.isVisible = false
        binding.resolutionIndicator.isVisible = false
        usedLiveIndicator?.isVisible = false
        usedLiveIndicator?.text = context.getString(R.string.app_widget___live)
        richPlayer.setVideoTextureView(binding.richSurface)
        binding.richSurface.alpha = 0f
        val mediaItem = MediaItem.fromUri(state.uri)
        val dataSourceFactory = DefaultHttpDataSource.Factory()
        state.authHeader?.let {
            dataSourceFactory.setDefaultRequestProperties(mapOf("Authorization" to it))
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        richPlayer.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        richPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        richPlayer.prepare()
        richPlayer.play()

        lastRichPlayerListener?.let(richPlayer::removeListener)
        lastRichPlayerAnalyticsListener?.let(richPlayer::removeAnalyticsListener)

        grabBitmap = { binding.richSurface.bitmap }

        lastRichPlayerListener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                binding.matrixView.matrixInput = MatrixView.MatrixInput(
                    flipH = state.flipH,
                    flipV = state.flipV,
                    rotate90 = state.rotate90,
                    contentHeight = videoSize.height,
                    contentWidth = videoSize.width,
                )
                dispatchNativeContentDimensionChanged(videoSize.width, videoSize.height, state.rotate90)
            }
        }.also {
            richPlayer.addListener(it)
        }

        lastRichPlayerAnalyticsListener = object : AnalyticsListener {
            override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
                super.onIsPlayingChanged(eventTime, isPlaying)
                Timber.v("onIsPlayingChanged: $isPlaying")

                binding.loadingState.isVisible = !isPlaying

                if (isPlaying) {
                    binding.errorState.isVisible = false
                    binding.reconnectingState.isVisible = false
                    binding.richSurface.alpha = 1f

                    liveIndicatorJob?.cancel()
                    liveIndicatorJob = coroutineScope.launchWhenCreated {
                        while (isActive) {
                            val isLive = richPlayer.isCurrentMediaItemLive
                            val delay = richPlayer.currentLiveOffset
                            usedLiveIndicator?.isVisible = isLive && delay < LIVE_DELAY_THRESHOLD_MS
                            delay(1000)
                        }
                    }
                } else {
                    liveIndicatorJob?.cancel()
                    usedLiveIndicator?.isVisible = false
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
        }.also {
            richPlayer.addAnalyticsListener(it)
        }
    } catch (e: Exception) {
        Timber.e(e)
        this.state = WebcamState.Error(state.uri.toString())
    }

    private fun displayMjpegFrame(newState: WebcamState.MjpegFrameReady) {
        binding.matrixView.matrixInput = MatrixView.MatrixInput(
            flipH = newState.flipH,
            flipV = newState.flipV,
            rotate90 = newState.rotate90,
            contentHeight = newState.frame.height,
            contentWidth = newState.frame.width,
        )

        dispatchNativeContentDimensionChanged(newState.frame.width, newState.frame.height, newState.rotate90)

        grabBitmap = { newState.frame }

        binding.playingState.isVisible = true
        binding.richSurface.isVisible = false
        binding.mjpegSurface.isVisible = true
        usedLiveIndicator?.isVisible = true
        binding.loadingState.isVisible = false
        binding.streamStalledIndicator.isVisible = false
        binding.mjpegSurface.setImageBitmap(newState.frame)

        // Hide live indicator if no new frame arrives within 3s
        // Show stalled indicator if no new frame arrives within 10s
        liveIndicatorJob?.cancel()
        liveIndicatorJob = coroutineScope.launchWhenCreated {
            val start = System.currentTimeMillis()

            // Wait until stream is stalled if this job is not cancelled (aka new frame arrived)
            if (newState.nextFrameDelayMs == null) {
                usedLiveIndicator?.text = context.getString(R.string.app_widget___live)
                delay(LIVE_DELAY_THRESHOLD_MS)
                usedLiveIndicator?.isVisible = false
                delay(STALLED_THRESHOLD_MS - LIVE_DELAY_THRESHOLD_MS)
            } else {
                val end = (start + newState.nextFrameDelayMs * 1.2f).toLong()
                while (end > System.currentTimeMillis()) {
                    val nextFrameIn = (newState.nextFrameDelayMs - (System.currentTimeMillis() - start)).coerceIn(0, 9000) / 1000
                    usedLiveIndicator?.text = context.getString(R.string.app_widget___live_x_seconds, nextFrameIn)
                    delay(1_000)
                }
            }

            // Stream is now stalled!
            binding.streamStalledIndicator.isVisible = true
            do {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)
                binding.streamStalledIndicatorDetail.text = context.getString(R.string.no_frames_since_xs, seconds)
                delay(1000)
            } while (isActive)
        }

        invalidate()
    }

    private fun captureBitmap() = suspend {
        binding.matrixView.matrixInput?.let { mi ->
            grabBitmap()?.let { bitmap ->
                val matrix = Matrix()
                if (mi.flipV) matrix.postScale(1f, -1f)
                if (mi.flipH) matrix.postScale(-1f, 1f)
                if (mi.rotate90) matrix.postRotate(-90f)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        }
    }

    private var View.isVisible: Boolean
        get() = visibility == View.VISIBLE
        set(value) {
            if ((value && visibility != View.VISIBLE) || (!value && visibility != View.GONE)) {
                visibility = if (value) View.VISIBLE else View.GONE
            }
        }

    sealed class WebcamState {
        object Loading : WebcamState()
        object Reconnecting : WebcamState()
        object NotConfigured : WebcamState()
        object RichStreamDisabled : WebcamState()
        data class Error(val streamUrl: String?) : WebcamState()

        data class RichStreamReady(
            val uri: Uri,
            val authHeader: String?,
            val flipH: Boolean,
            val flipV: Boolean,
            val rotate90: Boolean,
        ) : WebcamState()

        data class MjpegFrameReady(
            val frame: Bitmap,
            val flipH: Boolean,
            val flipV: Boolean,
            val rotate90: Boolean,
            val nextFrameDelayMs: Long?,
        ) : WebcamState()
    }
}
