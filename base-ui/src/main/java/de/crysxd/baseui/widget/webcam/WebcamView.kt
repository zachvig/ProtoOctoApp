package de.crysxd.baseui.widget.webcam

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
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
import kotlin.math.min

class WebcamView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attributeSet, defStyle) {

    private val binding = WebcamViewBinding.inflate(LayoutInflater.from(context), this)
    lateinit var coroutineScope: LifecycleCoroutineScope
    private var hideLiveIndicatorJob: Job? = null

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
        }
    var fullscreenIconResource: Int
        get() = 0
        set(value) {
            binding.imageButtonFullscreen.setImageResource(value)
        }
    var usedLiveIndicator: View? = null
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
    }

    @SuppressLint("SetTextI18n")
    private fun dispatchNativeContentDimensionChanged(width: Int, height: Int, rotate90: Boolean) {
        val w = if (rotate90) height else width
        val h = if (rotate90) width else height
        val ratio = "$w:$h"
        Timber.i("native aspect ratio: $ratio $rotate90")
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

    fun onPause() {
        Timber.i("Stopping stream")
        richPlayer.pause()
        richPlayer.stop()
    }

    private fun applyState(oldState: WebcamState?, newState: WebcamState) {
        if (newState == oldState && newState !is WebcamState.RichStreamReady) {
            return
        }

        hideLiveIndicatorJob?.cancel()
        if (oldState == null || oldState::class != newState::class) {
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
        richPlayer.setVideoTextureView(binding.richSurface)
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

        lastNativeWidth = null
        lastNativeHeight = null
        lastRichPlayerListener?.let(richPlayer::removeListener)
        lastRichPlayerAnalyticsListener?.let(richPlayer::removeAnalyticsListener)

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
                usedLiveIndicator?.isVisible = isPlaying
                binding.loadingState.isVisible = !isPlaying
                if (isPlaying) {
                    binding.errorState.isVisible = false
                    binding.reconnectingState.isVisible = false
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
        }.also {
            richPlayer.addAnalyticsListener(it)
        }
    } catch (e: Exception) {
        Timber.e(e)
        this.state = WebcamState.Error(state.uri.toString())
    }

    private fun invalidateMjpegFrame(frame: Bitmap) {
        binding.playingState.isVisible = true
        binding.richSurface.isVisible = false
        binding.mjpegSurface.isVisible = true
        usedLiveIndicator?.isVisible = true
        binding.loadingState.isVisible = false
        binding.streamStalledIndicator.isVisible = false
        binding.mjpegSurface.setImageBitmap(frame)

        // Hide live indicator if no new frame arrives within 3s
        // Show stalled indicator if no new frame arrives within 10s
        hideLiveIndicatorJob?.cancel()
        hideLiveIndicatorJob = coroutineScope.launchWhenCreated {
            val start = System.currentTimeMillis()
            delay(NOT_LIVE_IF_NO_FRAME_FOR_MS)
            usedLiveIndicator?.isVisible = false

            delay(STALLED_IF_NO_FRAME_FOR_MS - NOT_LIVE_IF_NO_FRAME_FOR_MS)
            binding.streamStalledIndicator.isVisible = true
            do {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)
                binding.streamStalledIndicatorDetail.text = context.getString(R.string.no_frames_since_xs, seconds)
                delay(1000)
            } while (isActive)
        }

        invalidate()
    }

    private fun displayMjpegFrame(newState: WebcamState.MjpegFrameReady) {
        binding.matrixView.matrixInput = MatrixView.MatrixInput(
            flipH = newState.flipH,
            flipV = newState.flipV,
            rotate90 = newState.rotate90,
            contentHeight = newState.frame.height,
            contentWidth = newState.frame.width,
        )

        lastNativeWidth = null
        lastNativeHeight = null
        dispatchNativeContentDimensionChanged(newState.frame.width, newState.frame.height, newState.rotate90)
        invalidateMjpegFrame(newState.frame)
    }

    fun requestSizeTransition() {
        binding.matrixView.beginInternalSizeTransition()
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
        ) : WebcamState()
    }
}
