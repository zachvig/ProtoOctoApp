package de.crysxd.octoapp.base.ui.widget.webcam

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.databinding.WebcamWidgetBinding
import de.crysxd.octoapp.base.di.injectActivityViewModel
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.webcam.WebcamSettingsMenu
import de.crysxd.octoapp.base.ui.widget.BaseWidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamViewModel.UiState
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamViewModel.UiState.Error
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamViewModel.UiState.Loading
import timber.log.Timber

const val NOT_LIVE_IF_NO_FRAME_FOR_MS = 3000L
const val STALLED_IF_NO_FRAME_FOR_MS = 5000L

class WebcamWidget(context: Context) : RecyclableOctoWidget<WebcamWidgetBinding, WebcamViewModel>(context) {
    override val binding = WebcamWidgetBinding.inflate(LayoutInflater.from(context))
    private var lastAspectRatio: String? = null
    private val observer = Observer(::onUiStateChanged)

    init {
        binding.webcamView.onResetConnection = {
            if (binding.webcamView.state == WebcamView.WebcamState.HlsStreamDisabled) {
                OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "hls_webcam_widget"))
                UriLibrary.getPurchaseUri().open(parent.requireOctoActivity())
            } else {
                baseViewModel.connect()
            }
        }
        binding.webcamView.onFullscreenClicked = ::openFullscreen
        binding.webcamView.supportsToubleShooting = true
        binding.webcamView.onScaleToFillChanged = {
            baseViewModel.storeScaleType(
                if (binding.webcamView.scaleToFill) {
                    ImageView.ScaleType.CENTER_CROP
                } else {
                    ImageView.ScaleType.FIT_CENTER
                },
                isFullscreen = false
            )
        }
        binding.webcamView.onSwitchWebcamClicked = { baseViewModel.nextWebcam() }
    }

    override fun getActionIcon() = R.drawable.ic_round_settings_24

    override fun onAction() {
        MenuBottomSheetFragment.createForMenu(WebcamSettingsMenu()).show(parent.childFragmentManager)
    }

    override fun createNewViewModel(parent: BaseWidgetHostFragment) = parent.injectActivityViewModel<WebcamViewModel>().value
    override fun getTitle(context: Context) = context.getString(R.string.webcam)
    override fun getAnalyticsName() = "webcam"

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        Timber.i("Resume")
        binding.webcamView.scaleToFill = baseViewModel.getScaleType(isFullscreen = false, ImageView.ScaleType.FIT_CENTER) != ImageView.ScaleType.FIT_CENTER
        binding.webcamView.coroutineScope = lifecycleOwner.lifecycleScope
        binding.webcamView.onResolutionClicked = { onAction() }
        applyAspectRatio(baseViewModel.getInitialAspectRatio())
        baseViewModel.uiState.observe(lifecycleOwner, observer)
    }

    private fun onUiStateChanged(state: UiState) {
        binding.webcamView.canSwitchWebcam = state.canSwitchWebcam
        binding.webcamView.state = when (state) {
            is Loading -> WebcamView.WebcamState.Loading
            UiState.WebcamNotConfigured -> WebcamView.WebcamState.NotConfigured
            is UiState.HlsStreamDisabled -> WebcamView.WebcamState.HlsStreamDisabled
            is UiState.FrameReady -> {
                applyAspectRatio(state.aspectRation)
                WebcamView.WebcamState.MjpegFrameReady(
                    frame = state.frame,
                    flipH = state.flipH,
                    flipV = state.flipV,
                    rotate90 = state.rotate90
                )
            }
            is UiState.HlsStreamReady -> {
                applyAspectRatio(state.aspectRation)
                WebcamView.WebcamState.HlsStreamReady(state.uri, state.authHeader)
            }
            is Error -> {
                state.aspectRation?.let(::applyAspectRatio)
                if (state.isManualReconnect) {
                    WebcamView.WebcamState.Error(state.streamUrl)
                } else {
                    WebcamView.WebcamState.Reconnecting
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.webcamView.onPause()
        baseViewModel.uiState.removeObserver(observer)
    }

    private fun openFullscreen() {
        UriLibrary.getWebcamUri().open(parent.requireOctoActivity())
        recordInteraction()
    }

    private fun applyAspectRatio(aspectRation: String) {
        if (lastAspectRatio != null && aspectRation != lastAspectRatio) {
            parent.requestTransition()
        }

        if (lastAspectRatio != aspectRation) {
            Timber.i("Applying aspect ratio: $aspectRation")
            lastAspectRatio = aspectRation
            ConstraintSet().also {
                it.clone(binding.webcamContent)
                it.setDimensionRatio(
                    R.id.webcamView,
                    aspectRation
                )
            }.applyTo(binding.webcamContent)
        }
    }
}