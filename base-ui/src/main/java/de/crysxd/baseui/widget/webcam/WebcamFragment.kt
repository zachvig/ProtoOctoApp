package de.crysxd.baseui.widget.webcam

import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.crysxd.baseui.InsetAwareScreen
import de.crysxd.baseui.R
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.databinding.WebcamFragmentBinding
import de.crysxd.baseui.di.injectActivityViewModel
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.baseui.menu.webcam.WebcamSettingsMenu
import de.crysxd.octoapp.base.data.models.ProgressWidgetSettings
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber


class WebcamFragment : Fragment(), InsetAwareScreen {

    private val viewModel: WebcamViewModel by injectActivityViewModel()
    private val orientationViewModel by lazy { ViewModelProvider(this)[OrientationViewModel::class.java] }
    private lateinit var binding: WebcamFragmentBinding
    private var systemUiFlagsBackup = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        WebcamFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orientationViewModel.init(requireOctoActivity().requestedOrientation)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("create")

        binding.webcamView.coroutineScope = viewLifecycleOwner.lifecycleScope
        binding.webcamView.onResetConnection = viewModel::connect
        binding.webcamView.fullscreenIconResource = R.drawable.ic_round_fullscreen_exit_24
        binding.webcamView.usedLiveIndicator = binding.externalLiveIndicator
        binding.webcamView.onResolutionClicked = {
            MenuBottomSheetFragment.createForMenu(WebcamSettingsMenu()).show(childFragmentManager)
        }
        binding.webcamView.onScaleToFillChanged = {
            viewModel.storeScaleType(
                if (it) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER,
                isFullscreen = true
            )
        }
        binding.webcamView.onShareImageClicked = { viewModel.shareImage(requireContext(), it) }
        binding.webcamView.onSwitchWebcamClicked = { viewModel.nextWebcam() }
        binding.webcamView.scaleToFill = viewModel.getScaleType(isFullscreen = true, ImageView.ScaleType.FIT_CENTER) != ImageView.ScaleType.FIT_CENTER
        binding.webcamView.onFullscreenClicked = {
            findNavController().popBackStack()
        }

        // Handle orientation stuff
        binding.webcamView.onNativeAspectRatioChanged = { ratio, width, height ->
            viewModel.storeAspectRatio(ratio)
            val frameAspectRatio = width / height.toFloat()
            val screenAspectRatio = resources.displayMetrics.run { widthPixels / heightPixels.toFloat() }

            orientationViewModel.preferredOrientation = if ((frameAspectRatio < 1 && screenAspectRatio > 1) || (frameAspectRatio > 1 && screenAspectRatio < 1)) {
                // Oh no! if we rotate the screen, the image would fit better!
                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            } else {
                // Aspect ratio of screen and frame match, do not change
                requireActivity().requestedOrientation
            }
            requireActivity().requestedOrientation = orientationViewModel.preferredOrientation
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            binding.webcamView.canSwitchWebcam = it.canSwitchWebcam
            binding.webcamView.state = when (it) {
                is WebcamViewModel.UiState.Loading -> WebcamView.WebcamState.Loading
                WebcamViewModel.UiState.WebcamNotConfigured -> WebcamView.WebcamState.NotConfigured
                is WebcamViewModel.UiState.RichStreamDisabled -> {
                    // We can't launch the purchase flow in fullscreen. Close screen.
                    findNavController().popBackStack()
                    WebcamView.WebcamState.RichStreamDisabled
                }
                is WebcamViewModel.UiState.FrameReady -> WebcamView.WebcamState.MjpegFrameReady(
                    frame = it.frame,
                    flipH = it.flipH,
                    flipV = it.flipV,
                    rotate90 = it.rotate90
                )
                is WebcamViewModel.UiState.RichStreamReady -> WebcamView.WebcamState.RichStreamReady(
                    uri = it.uri,
                    authHeader = it.authHeader,
                    flipH = it.flipH,
                    flipV = it.flipV,
                    rotate90 = it.rotate90
                )
                is WebcamViewModel.UiState.Error -> if (it.isManualReconnect) {
                    WebcamView.WebcamState.Error(it.streamUrl)
                } else {
                    WebcamView.WebcamState.Reconnecting
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            val compactEtaDate = BaseInjector.get().octoPreferences().progressWidgetSettings.etaStyle == ProgressWidgetSettings.EtaStyle.Compact
            BaseInjector.get().octoPrintProvider().passiveCurrentMessageFlow("webcam").collectLatest { message ->
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
                        getString(R.string.time_left_x, BaseInjector.get().formatDurationUseCase().execute(it))
                    }
                    binding.textViewEta.text = message.progress?.printTimeLeft?.let {
                        BaseInjector.get().formatEtaUseCase().execute(FormatEtaUseCase.Params(it.toLong(), useCompactDate = compactEtaDate))
                    }
                } else {
                    binding.textViewProgress.text = ""
                    binding.textViewTimeLeft.text = ""
                    binding.textViewEta.text = ""
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        requireOctoActivity().octo.isVisible = false
        requireActivity().requestedOrientation = orientationViewModel.preferredOrientation

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = requireActivity().window.decorView.systemUiVisibility
            systemUiFlagsBackup = flags
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            requireActivity().window.decorView.systemUiVisibility = flags
        }
    }

    override fun onPause() {
        super.onPause()
        binding.webcamView.onPause()
    }

    override fun onStop() {
        super.onStop()
        requireActivity().requestedOrientation = orientationViewModel.requestedOrientationBackup
        requireActivity().window.decorView.systemUiVisibility = systemUiFlagsBackup
    }

    override fun handleInsets(insets: Rect) {
        binding.root.updatePadding(
            top = insets.top,
            bottom = insets.bottom,
            left = insets.left,
            right = insets.right
        )
    }

    class OrientationViewModel : ViewModel() {
        private val default = -1000
        var preferredOrientation = default
        var requestedOrientationBackup = 0
            private set

        fun init(orientation: Int) {
            if (preferredOrientation == default) {
                preferredOrientation = orientation
                requestedOrientationBackup = orientation
            }
        }
    }
}
