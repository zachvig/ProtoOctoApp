package de.crysxd.octoapp.base.ui.widget.webcam

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidgetViewModel.UiState
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidgetViewModel.UiState.Error
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidgetViewModel.UiState.Loading
import kotlinx.android.synthetic.main.widget_webcam.view.*

class WebcamWidget(parent: Fragment) : OctoWidget(parent) {

    val viewModel: WebcamWidgetViewModel by injectViewModel()

    override fun getTitle(context: Context) = "Webcam"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.widget_webcam, container, false)

    override fun onViewCreated(view: View) {
        viewModel.uiState.observe(viewLifecycleOwner, Observer(this::onUiStateChanged))
    }

    private fun onUiStateChanged(state: UiState) {
        view.erroIndicator.isVisible = false
        when (state) {
            Loading -> {
                view.loadingIndicator.isVisible = true
            }

            is UiState.FrameReady -> {
                view.loadingIndicator.isVisible = false
                (view.streamView.drawable as? BitmapDrawable)?.bitmap?.recycle()
                view.streamView.setImageBitmap(state.frame)
            }

            Error -> {
                view.loadingIndicator.isVisible = false
                view.erroIndicator.isVisible = true
            }
        }
    }
}