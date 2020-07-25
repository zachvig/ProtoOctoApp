package de.crysxd.octoapp.base.ui.widget.webcam

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidgetViewModel.UiState
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidgetViewModel.UiState.Error
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidgetViewModel.UiState.Loading
import kotlinx.android.synthetic.main.widget_webcam.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

const val NOT_LIVE_IF_NO_FRAME_FOR_MS = 3000L

class WebcamWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: WebcamWidgetViewModel by injectViewModel()
    private var hideLiveIndicatorJob: Job? = null

    override fun getTitle(context: Context) = context.getString(R.string.webcam)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.widget_webcam, container, false)

    override fun onViewCreated(view: View) {
        viewModel.uiState.observe(viewLifecycleOwner, Observer(this::onUiStateChanged))
    }

    private fun onUiStateChanged(state: UiState) {
        view.erroIndicator.isVisible = false
        view.errorIndicatorManual.isVisible = false
        view.liveIndicator.isVisible = false

        when (state) {
            Loading -> {
                view.loadingIndicator.isVisible = true
            }

            is UiState.FrameReady -> {
                view.liveIndicator.isVisible = true
                view.loadingIndicator.isVisible = false
                (view.streamView.drawable as? BitmapDrawable)?.bitmap?.recycle()
                view.streamView.setImageBitmap(state.frame)

                // Hide live indicator if no new frame arrives within 3s
                hideLiveIndicatorJob?.cancel()
                view.liveIndicator.animate().alpha(1f).start()
                hideLiveIndicatorJob = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                    delay(NOT_LIVE_IF_NO_FRAME_FOR_MS)
                    view.liveIndicator.animate().alpha(0f).start()
                }
            }

            is Error -> {
                view.liveIndicator.isVisible = false
                view.loadingIndicator.isVisible = false
                view.erroIndicator.isVisible = !state.isManualReconnect
                view.errorIndicatorManual.isVisible = state.isManualReconnect
            }
        }
    }
}