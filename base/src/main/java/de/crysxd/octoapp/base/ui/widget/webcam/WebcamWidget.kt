package de.crysxd.octoapp.base.ui.widget.webcam

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import de.crysxd.octoapp.base.ui.webcam.WebcamView
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamViewModel.UiState
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamViewModel.UiState.Error
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamViewModel.UiState.Loading
import kotlinx.android.synthetic.main.widget_webcam.*
import kotlinx.android.synthetic.main.widget_webcam.view.*

const val NOT_LIVE_IF_NO_FRAME_FOR_MS = 3000L
const val STALLED_IF_NO_FRAME_FOR_MS = 5000L

class WebcamWidget(
    parent: Fragment,
    private val isFullscreen: Boolean = false
) : OctoWidget(parent) {
    private val viewModel: WebcamViewModel by injectViewModel()

    override fun getTitle(context: Context) = context.getString(R.string.webcam)
    override fun getAnalyticsName() = "webcam"

    @SuppressLint("ClickableViewAccessibility")
    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup) = inflater.suspendedInflate(R.layout.widget_webcam, container, false) as ViewGroup


    override fun onViewCreated(view: View) {
        applyAspectRatio(viewModel.getInitialAspectRatio())
        webcamView.coroutineScope = parent.viewLifecycleOwner.lifecycleScope
        webcamView.onResetConnection = viewModel::connect
        webcamView.onFullscreenClicked = ::openFullscreen
        viewModel.uiState.observe(parent, ::onUiStateChanged)
    }

    private fun onUiStateChanged(state: UiState) {
        webcamView.state = when (state) {
            Loading -> WebcamView.WebcamState.Loading
            UiState.WebcamNotConfigured -> WebcamView.WebcamState.NotConfigured
            is UiState.FrameReady -> {
                applyAspectRatio(state.aspectRation)
                WebcamView.WebcamState.MjpegFrameReady(state.frame)
            }
            is UiState.HlsStreamReady -> {
                applyAspectRatio(state.aspectRation)
                WebcamView.WebcamState.HlsStreamReady(state.uri)
            }
            is Error -> if (state.isManualReconnect) {
                WebcamView.WebcamState.Error
            } else {
                WebcamView.WebcamState.Reconnecting
            }
        }
    }

    private fun openFullscreen() {
        FullscreenWebcamActivity.start(parent.requireActivity())
    }

    private fun applyAspectRatio(aspectRation: String) {
        ConstraintSet().also {
            it.clone(view.webcamContent)
            it.setDimensionRatio(
                R.id.webcamView,
                if (isFullscreen) {
                    null
                } else {
                    aspectRation
                }
            )
        }.applyTo(view.webcamContent)
    }
}