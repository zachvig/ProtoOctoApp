package de.crysxd.octoapp.base.ui.widget.webcam

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import kotlinx.android.synthetic.main.fragment_webcam.*
import kotlinx.coroutines.flow.collectLatest

class WebcamFragment : Fragment(R.layout.fragment_webcam) {

    val widget = WebcamWidget(this@WebcamFragment, true)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenCreated {
            val widgetView = widget.getView(requireContext(), view as ViewGroup)
            widget.externalLiveIndicator = liveIndicator
            view.addView(widgetView, 0)

            widget.uiState.observe(viewLifecycleOwner) {
                if (it is WebcamWidgetViewModel.UiState.FrameReady) {
                    val frameAspectRatio = it.frame.width / it.frame.height.toFloat()
                    val screenAspectRatio = resources.displayMetrics.run { widthPixels / heightPixels.toFloat() }
                    val orientation = resources.configuration.orientation

                    requireActivity().requestedOrientation = if ((frameAspectRatio < 1 && screenAspectRatio > 1) || (frameAspectRatio > 1 && screenAspectRatio < 1)) {
                        // Oh no! if we rotate the screen, the image would fit better!
                        ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                    } else {
                        // Aspect ratio of screen and frame match, do not change
                        requireActivity().requestedOrientation
                    }
                }
            }

            Injector.get().octoPrintProvider().passiveCurrentMessageFlow().collectLatest { message ->
                val flags = message.state?.flags
                val printActive = listOf(flags?.paused, flags?.pausing, flags?.printing, flags?.cancelling).any { it == true }

                if (printActive) {
                    textViewProgress.text = when {
                        flags?.pausing == true -> getString(R.string.pausing)
                        flags?.cancelling == true -> getString(R.string.pausing)
                        flags?.printing == true && message.progress?.completion != null -> getString(R.string.x_percent, message.progress?.completion)
                        else -> ""
                    }
                    textViewTimeLeft.text = message.progress?.printTimeLeft?.toLong()?.let {
                        getString(R.string.time_left_x, Injector.get().formatDurationUseCase().execute(it))
                    }
                    textViewEta.text = message.progress?.printTimeLeft?.let {
                        getString(R.string.eta_x, Injector.get().formatEtaUseCase().execute(it))
                    }
                } else {
                    textViewProgress.text = ""
                    textViewTimeLeft.text = ""
                    textViewEta.text = ""
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        widget.onResume()
    }
}