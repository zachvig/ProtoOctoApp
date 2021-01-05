package de.crysxd.octoapp.base.ui.widget.webcam

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
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
        webcamView.onResetConnection = {
            if (webcamView.state == WebcamView.WebcamState.HlsStreamDisabled) {
                OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "hls_webcam_widget"))
                view.findNavController().navigate(R.id.action_show_purchase_flow)
            } else {
                viewModel.connect()
            }
        }
        webcamView.onFullscreenClicked = ::openFullscreen
        webcamView.onScaleToFillChanged = {
            viewModel.storeScaleType(
                if (webcamView.scaleToFill) {
                    ImageView.ScaleType.CENTER_CROP
                } else {
                    ImageView.ScaleType.FIT_CENTER
                },
                isFullscreen = false
            )
        }
        webcamView.scaleToFill = viewModel.getScaleType(isFullscreen = false, ImageView.ScaleType.FIT_CENTER) != ImageView.ScaleType.FIT_CENTER
        viewModel.uiState.observe(parent, ::onUiStateChanged)
    }

    private fun onUiStateChanged(state: UiState) {
        webcamView.state = when (state) {
            Loading -> WebcamView.WebcamState.Loading
            UiState.WebcamNotConfigured -> WebcamView.WebcamState.NotConfigured
            UiState.HlsStreamDisabled -> WebcamView.WebcamState.HlsStreamDisabled
            is UiState.FrameReady -> {
                applyAspectRatio(state.aspectRation)
                WebcamView.WebcamState.MjpegFrameReady(state.frame)
            }
            is UiState.HlsStreamReady -> {
                applyAspectRatio(state.aspectRation)
                WebcamView.WebcamState.HlsStreamReady(state.uri)
            }
            is Error -> if (state.isManualReconnect) {
                WebcamView.WebcamState.Error(state.streamUrl)
            } else {
                WebcamView.WebcamState.Reconnecting
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.connect()
    }

    override fun onPause() {
        super.onPause()
        webcamView.onPause()
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