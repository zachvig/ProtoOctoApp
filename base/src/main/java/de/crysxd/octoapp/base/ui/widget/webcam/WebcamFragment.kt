package de.crysxd.octoapp.base.ui.widget.webcam

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.WebcamFragmentBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import kotlinx.coroutines.flow.collectLatest

class WebcamFragment : Fragment(R.layout.webcam_fragment) {

    private val viewModel: WebcamViewModel by injectViewModel()
    private lateinit var binding: WebcamFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        WebcamFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webcamView.coroutineScope = viewLifecycleOwner.lifecycleScope
        binding.webcamView.onResetConnection = viewModel::connect
        binding.webcamView.fullscreenIconResource = R.drawable.ic_round_fullscreen_exit_24
        binding.webcamView.usedLiveIndicator = binding.externalLiveIndicator
        binding.webcamView.onScaleToFillChanged = {
            viewModel.storeScaleType(
                if (binding.webcamView.scaleToFill) {
                    ImageView.ScaleType.CENTER_CROP
                } else {
                    ImageView.ScaleType.FIT_CENTER
                },
                isFullscreen = true
            )
        }
        binding.webcamView.scaleToFill = viewModel.getScaleType(isFullscreen = true, ImageView.ScaleType.FIT_CENTER) != ImageView.ScaleType.FIT_CENTER
        binding.webcamView.onFullscreenClicked = {
            requireActivity().finish()
        }

        // Handle orientation stuff
        binding.webcamView.onNativeAspectRatioChanged = { width, height ->
            val frameAspectRatio = width / height.toFloat()
            val screenAspectRatio = resources.displayMetrics.run { widthPixels / heightPixels.toFloat() }

            requireActivity().requestedOrientation = if ((frameAspectRatio < 1 && screenAspectRatio > 1) || (frameAspectRatio > 1 && screenAspectRatio < 1)) {
                // Oh no! if we rotate the screen, the image would fit better!
                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            } else {
                // Aspect ratio of screen and frame match, do not change
                requireActivity().requestedOrientation
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            binding.webcamView.state = when (it) {
                WebcamViewModel.UiState.Loading -> WebcamView.WebcamState.Loading
                WebcamViewModel.UiState.WebcamNotConfigured -> WebcamView.WebcamState.NotConfigured
                WebcamViewModel.UiState.HlsStreamDisabled -> {
                    // We can't launch the purchase flow in fullscreen. Finish activity.
                    requireActivity().finish()
                    WebcamView.WebcamState.HlsStreamDisabled
                }
                is WebcamViewModel.UiState.FrameReady -> WebcamView.WebcamState.MjpegFrameReady(it.frame)
                is WebcamViewModel.UiState.HlsStreamReady -> WebcamView.WebcamState.HlsStreamReady(it.uri)
                is WebcamViewModel.UiState.Error -> if (it.isManualReconnect) {
                    WebcamView.WebcamState.Error(it.streamUrl)
                } else {
                    WebcamView.WebcamState.Reconnecting
                }
            }
        }

        viewModel.connect()

        lifecycleScope.launchWhenCreated {
            Injector.get().octoPrintProvider().passiveCurrentMessageFlow().collectLatest { message ->
                val flags = message.state?.flags
                val printActive = listOf(flags?.paused, flags?.pausing, flags?.printing, flags?.cancelling).any { it == true }

                if (printActive) {
                    binding.textViewProgress.text = when {
                        flags?.pausing == true -> getString(R.string.pausing)
                        flags?.cancelling == true -> getString(R.string.pausing)
                        flags?.printing == true && message.progress?.completion != null -> getString(R.string.x_percent, message.progress?.completion)
                        else -> ""
                    }
                    binding.textViewTimeLeft.text = message.progress?.printTimeLeft?.toLong()?.let {
                        getString(R.string.time_left_x, Injector.get().formatDurationUseCase().execute(it))
                    }
                    binding.textViewEta.text = message.progress?.printTimeLeft?.let {
                        Injector.get().formatEtaUseCase().execute(FormatEtaUseCase.Params(it.toLong(), false))
                    }
                } else {
                    binding.textViewProgress.text = ""
                    binding.textViewTimeLeft.text = ""
                    binding.textViewEta.text = ""
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.connect()
    }

    override fun onPause() {
        super.onPause()
        binding.webcamView.onPause()
    }
}
