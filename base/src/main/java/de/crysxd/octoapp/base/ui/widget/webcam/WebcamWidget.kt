package de.crysxd.octoapp.base.ui.widget.webcam

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidgetViewModel.UiState
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidgetViewModel.UiState.Error
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidgetViewModel.UiState.Loading
import kotlinx.android.synthetic.main.widget_webcam.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

const val NOT_LIVE_IF_NO_FRAME_FOR_MS = 3000L
const val STALLED_IF_NO_FRAME_FOR_MS = 5000L

class WebcamWidget(
    parent: Fragment,
    private val isFullscreen: Boolean = false
) : OctoWidget(parent) {

    private val viewModel: WebcamWidgetViewModel by injectViewModel()
    private var hideLiveIndicatorJob: Job? = null
    private var lastState: UiState? = null

    override fun getTitle(context: Context) = context.getString(R.string.webcam)
    override fun getAnalyticsName() = "webcam"

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.suspendedInflate(R.layout.widget_webcam, container, false) as ViewGroup

        // Do not use the card view in fullscreen
        return if (isFullscreen) {
            val webcamContent = view.webcamContent
            view.removeView(webcamContent)
            webcamContent
        } else {
            view
        }
    }

    override fun onViewCreated(view: View) {
        viewModel.uiState.observe(parent, Observer(this::onUiStateChanged))
        view.buttonReconnect.setOnClickListener { viewModel.connect() }

        // Fullscreen button
        view.imageButtonFullscreen.setOnClickListener {
            recordInteraction()
            if (isFullscreen) {
                parent.activity?.finish()
            } else {
                FullscreenWebcamActivity.start(parent.requireActivity())
            }
        }
        view.imageButtonFullscreen.setImageResource(
            if (isFullscreen) {
                R.drawable.ic_round_fullscreen_exit_24
            } else {
                R.drawable.ic_round_fullscreen_24
            }
        )

        applyAspectRatio(viewModel.getInitialAspectRatio())
    }

    private fun beginDelayedTransition() = TransitionManager.beginDelayedTransition(view.webcamContent, InstantAutoTransition())

    private fun onUiStateChanged(state: UiState) {
        if (lastState == null || state::class != lastState!!::class) {
            beginDelayedTransition()
        }

        view.erroIndicator.isVisible = false
        view.errorIndicatorManual.isVisible = false
        view.liveIndicator.isVisible = false
        view.streamStalledIndicator.isVisible = false
        view.notConfiguredIndicator.isVisible = false

        // Hide loading indicator in every state to prevent the animation to start over

        when (state) {
            Loading -> {
                view.loadingIndicator.isVisible = true
            }

            UiState.WebcamNotConfigured -> {
                view.loadingIndicator.isVisible = false
                view.notConfiguredIndicator.isVisible = true
            }

            is UiState.FrameReady -> {
                view.loadingIndicator.isVisible = false

                view.liveIndicator.isVisible = true
                view.streamView.setImageBitmap(state.frame)

                applyAspectRatio(state.aspectRation)

                // Hide live indicator if no new frame arrives within 3s
                // Show stalled indicator if no new frame arrives within 10s
                hideLiveIndicatorJob?.cancel()
                hideLiveIndicatorJob = parent.lifecycleScope.launchWhenCreated {
                    delay(NOT_LIVE_IF_NO_FRAME_FOR_MS)
                    beginDelayedTransition()
                    view.liveIndicator.isVisible = false

                    delay(STALLED_IF_NO_FRAME_FOR_MS - NOT_LIVE_IF_NO_FRAME_FOR_MS)
                    beginDelayedTransition()
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(STALLED_IF_NO_FRAME_FOR_MS)
                    view.streamStalledIndicatorDetail.text = requireContext().getString(R.string.no_frames_since_xs, seconds)
                    view.streamStalledIndicator.isVisible = true
                }
            }

            is Error -> {
                hideLiveIndicatorJob?.cancel()

                view.loadingIndicator.isVisible = false

                view.streamUrl1.text = state.streamUrl
                view.streamUrl2.text = state.streamUrl

                view.erroIndicator.isVisible = !state.isManualReconnect
                view.errorIndicatorManual.isVisible = state.isManualReconnect
            }
        }

        lastState = state
    }

    private fun applyAspectRatio(aspectRation: String) {
        ConstraintSet().also {
            it.clone(view.webcamContent)
            it.setDimensionRatio(
                R.id.streamView,
                if (isFullscreen) {
                    null
                } else {
                    aspectRation
                }
            )
        }.applyTo(view.webcamContent)
    }
}