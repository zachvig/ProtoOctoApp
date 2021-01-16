package de.crysxd.octoapp.base.ui.common.gcode

import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectActivityViewModel
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.gcode.render.GcodeRenderView
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.android.synthetic.main.fragment_gcode_render.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber
import java.text.DecimalFormat
import kotlin.math.roundToInt

const val NOT_LIVE_IF_NO_UPDATE_FOR_MS = 5000L

class GcodePreviewFragment : Fragment(R.layout.fragment_gcode_render) {

    companion object {
        private const val ARG_FILE = "file"
        private const val ARG_USE_LIVE = "useLive"
        private const val ARG_STANDALONE_SCREEN = "standaloneScreen"
        private const val LAYER_PROGRESS_STEPS = 1000

        fun createForFile(file: FileObject.File, useLive: Boolean) = GcodePreviewFragment().apply {
            arguments = bundleOf(ARG_FILE to file, ARG_USE_LIVE to useLive)
        }
    }

    private var forceUpdateSlidersOnNext = false
    private var hideLiveJob: Job? = null
    private val file get() = requireArguments().getSerializable(ARG_FILE) as FileObject.File
    private val useLive get() = requireArguments().getBoolean(ARG_USE_LIVE, true)
    private val isStandaloneScreen get() = requireArguments().getBoolean(ARG_STANDALONE_SCREEN)
    private val viewModel: GcodePreviewViewModel by injectActivityViewModel(Injector.get().viewModelFactory())
    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                pushSeekBarValuesToViewModel(seekBar)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.downloadGcode(file, false)

        if (useLive) {
            viewModel.useLiveProgress()
        } else {
            viewModel.useManualProgress(0, 1f)
            forceUpdateSlidersOnNext = true
        }

        downloadLargeFile.setOnClickListener { viewModel.downloadGcode(file, true) }
        retryButton.setOnClickListener { viewModel.downloadGcode(file, true) }
        downloadLargeFile.text = getString(R.string.download_x, file.size.asStyleFileSize())

        layerSeekBar.setOnSeekBarChangeListener(seekBarListener)
        layerProgressSeekBar.setOnSeekBarChangeListener(seekBarListener)

        nextLayerButton.setOnClickListener {
            layerSeekBar.progress = layerSeekBar.progress + 1
            pushSeekBarValuesToViewModel(layerSeekBar)
        }

        previousLayerButton.setOnClickListener {
            layerSeekBar.progress = layerSeekBar.progress - 1
            pushSeekBarValuesToViewModel(layerSeekBar)
        }

        if (isStandaloneScreen) {
            renderView.updatePadding(top = requireContext().resources.getDimensionPixelSize(R.dimen.common_view_top_padding))
        }

        syncButton.isVisible = isStandaloneScreen
        syncButtonSeparator.isVisible = isStandaloneScreen
        syncButton.setOnClickListener {
            (viewModel.viewState.value as? GcodePreviewViewModel.ViewState.DataReady)?.let { currentState ->
                if (currentState.isLive) {
                    pushSeekBarValuesToViewModel()
                } else {
                    viewModel.useLiveProgress()
                }
            }
        }

        buttonEnableFeature.setOnClickListener {
            OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "gcode_preview"))
            findNavController().navigate(R.id.action_show_purchase_flow)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.viewState.observe(viewLifecycleOwner, ::updateViewState)
        }
    }

    private fun pushSeekBarValuesToViewModel(seekBar: SeekBar? = null) {
        // Show entire layer if layer is changed
        if (seekBar == layerSeekBar) {
            layerProgressSeekBar.progress = LAYER_PROGRESS_STEPS
        }

        viewModel.useManualProgress(
            layer = layerSeekBar.progress,
            progress = layerProgressSeekBar.progress / LAYER_PROGRESS_STEPS.toFloat()
        )
    }

    override fun onStart() {
        super.onStart()

        if (isStandaloneScreen) {
            requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
            requireOctoActivity().octo.isVisible = false
        }
    }

    private fun updateViewState(state: GcodePreviewViewModel.ViewState) {
        TransitionManager.beginDelayedTransition(view as ViewGroup)

        renderGroup.isVisible = state is GcodePreviewViewModel.ViewState.DataReady
        largeFileGroup.isVisible = state is GcodePreviewViewModel.ViewState.LargeFileDownloadRequired
        errorGroup.isVisible = state is GcodePreviewViewModel.ViewState.Error
        featureDisabledGroup.isVisible = state is GcodePreviewViewModel.ViewState.FeatureDisabled

        when (state) {
            is GcodePreviewViewModel.ViewState.Loading -> {
                loadingGroup.isVisible = true
                progressBar.progress = (state.progress * 100).roundToInt()
                progressBar.isIndeterminate = state.progress == 0f
                Timber.v("Progress: ${state.progress}")
            }
            is GcodePreviewViewModel.ViewState.DataReady -> {
                loadingGroup.isVisible = false
                render(state)
                Timber.v("Ready")

            }
            is GcodePreviewViewModel.ViewState.Error -> {
                loadingGroup.isVisible = false
                requireOctoActivity().showDialog(state.exception)
                Timber.i("Error :(")
            }

            is GcodePreviewViewModel.ViewState.LargeFileDownloadRequired -> {
                loadingGroup.isVisible = false
                Timber.i("Large download required")
            }

            is GcodePreviewViewModel.ViewState.FeatureDisabled -> {
                loadingGroup.isVisible = false
                preview.setImageResource(state.renderStyle.background)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    preview.foreground = ContextCompat.getDrawable(requireContext(), R.drawable.gcode_preview)
                }
                Timber.i("Feature disabled")
            }
        }
    }

    private fun render(state: GcodePreviewViewModel.ViewState.DataReady) {
        if (state.renderContext == null || state.renderStyle == null || state.printerProfile == null) {
            Timber.e(IllegalStateException("Incomplete data ready state exposed ${state.renderContext == null} ${state.renderStyle == null} ${state.printerProfile == null}"))
            return
        }

        layerSeekBar.max = state.renderContext.layerCount - 1
        layerProgressSeekBar.max = LAYER_PROGRESS_STEPS
        if (state.isLive || forceUpdateSlidersOnNext) {
            forceUpdateSlidersOnNext = false
            layerSeekBar.progress = state.renderContext.layerNumber
            layerProgressSeekBar.progress = (state.renderContext.layerProgress * LAYER_PROGRESS_STEPS).roundToInt()
        }

        syncButton.setImageResource(
            if (state.isLive) {
                R.drawable.ic_round_sync_disabled_24
            } else {
                R.drawable.ic_round_sync_24
            }
        )

        live.isVisible = state.isLive
        hideLiveJob?.cancel()
        hideLiveJob = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            delay(NOT_LIVE_IF_NO_UPDATE_FOR_MS)
            live.isVisible = false
        }

        val layerHeightMm = DecimalFormat("0.0#").format(state.renderContext.layerZHeight)
        val layerProgressPercent = layerProgressSeekBar.progress / LAYER_PROGRESS_STEPS.toFloat()
        layerNumber.text = getString(R.string.x_of_y, layerSeekBar.progress + 1, state.renderContext.layerCount)
        layerHeight.text = getString(R.string.x_mm, layerHeightMm)
        layerProgress.text = String.format("%.0f %%", layerProgressPercent * 100)

        // Only switch to async render if the view recommends it.
        // This way we have smooth scrolling as long as possible but never block the UI thread
        if (renderView.asyncRenderRecommended && !renderView.useAsyncRender) {
            slow.animate().alpha(1f).start()
            Toast.makeText(requireContext(), "Slow", Toast.LENGTH_SHORT).show()
            renderView.enableAsyncRender(viewLifecycleOwner.lifecycleScope)
        }

        renderView.renderParams = GcodeRenderView.RenderParams(
            renderContext = state.renderContext,
            renderStyle = state.renderStyle,
            printBedSizeMm = PointF(state.printerProfile.volume.width, state.printerProfile.volume.depth),
            extrusionWidthMm = state.printerProfile.extruder.nozzleDiameter,
        )
    }

    private val GcodePreviewViewModel.ViewState.DataReady?.isLive get() = this?.fromUser != null && !fromUser
}