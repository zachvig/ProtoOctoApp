package de.crysxd.octoapp.base.ui.webcam

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.transition.TransitionManager
import com.google.android.exoplayer2.SimpleExoPlayer
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.ui.widget.webcam.NOT_LIVE_IF_NO_FRAME_FOR_MS
import de.crysxd.octoapp.base.ui.widget.webcam.STALLED_IF_NO_FRAME_FOR_MS
import kotlinx.android.synthetic.main.view_webcam.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class WebcamView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0) : FrameLayout(context, attributeSet, defStyle) {

    private val mtlPlayer by lazy { SimpleExoPlayer.Builder(context).build() }
    lateinit var coroutineScope: LifecycleCoroutineScope
    private var hideLiveIndicatorJob: Job? = null
    var state: WebcamState = WebcamState.Loading
        set(value) {
            field = value
            applyState()
        }

    init {
        View.inflate(context, R.layout.view_webcam, this)
        applyState()
    }

    private fun applyState() {
        children.forEach { it.isVisible = false }

        when (val localState = state) {
            WebcamState.Loading -> loadingState.isVisible = true
            WebcamState.NotConfigured -> notConfiguredState.isVisible = true
            WebcamState.Reconnecting -> reconnectingState.isVisible = true
            is WebcamState.Error -> errorState.isVisible = true
            is WebcamState.HlsStreamReady -> TODO()
            is WebcamState.MjpegFrameReady -> displayMjpegFrame(localState)
        }
    }

    private fun displayMjpegFrame(state: WebcamState.MjpegFrameReady) {
        playingState.isVisible = true
        hlsSurface.isVisible = false
        mjpegSurface.isVisible = true
        liveIndicator.isVisible = true
        mjpegSurface.setImageBitmap(state.frame)

        // Hide live indicator if no new frame arrives within 3s
        // Show stalled indicator if no new frame arrives within 10s
        hideLiveIndicatorJob?.cancel()
        hideLiveIndicatorJob = coroutineScope.launchWhenCreated {
            delay(NOT_LIVE_IF_NO_FRAME_FOR_MS)
            beginDelayedTransition()
            liveIndicator.isVisible = false

            delay(STALLED_IF_NO_FRAME_FOR_MS - NOT_LIVE_IF_NO_FRAME_FOR_MS)
            beginDelayedTransition()
            val seconds = TimeUnit.MILLISECONDS.toSeconds(STALLED_IF_NO_FRAME_FOR_MS)
            streamStalledIndicatorDetail.text = context.getString(R.string.no_frames_since_xs, seconds)
            streamStalledIndicator.isVisible = true
        }
    }

    private fun beginDelayedTransition() = TransitionManager.beginDelayedTransition(this, InstantAutoTransition())

    sealed class WebcamState {
        object Loading : WebcamState()
        object Reconnecting : WebcamState()
        object NotConfigured : WebcamState()
        data class Error(val rerty: () -> Unit) : WebcamState()
        data class HlsStreamReady(val uri: Uri) : WebcamState()
        data class MjpegFrameReady(val frame: Bitmap) : WebcamState()
    }
}