package de.crysxd.octoapp.base.ui.common.gcode

import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.gcode.render.GcodeRenderView
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.android.synthetic.main.fragment_gcode_render.*
import timber.log.Timber
import java.text.DecimalFormat
import kotlin.math.roundToInt

class GcodePreviewFragment : Fragment(R.layout.fragment_gcode_render) {

    companion object {
        private const val ARG_FILE = "file"
        private const val LAYER_PROGRESS_STEPS = 1000

        fun createForFile(file: FileObject.File) = GcodePreviewFragment().apply {
            arguments = bundleOf(ARG_FILE to file)
        }
    }

    private val file get() = requireArguments().getSerializable(ARG_FILE) as FileObject.File
    private val viewModel: GcodePreviewViewModel by injectViewModel(Injector.get().viewModelFactory())
    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                // Show entire layer if layer is changed
                if (seekBar == layerSeekBar) {
                    layerProgressSeekBar.progress = LAYER_PROGRESS_STEPS
                }

                viewModel.useManualProgress(
                    layer = layerSeekBar.progress,
                    progress = layerProgressSeekBar.progress / LAYER_PROGRESS_STEPS.toFloat()
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.downloadGcode(file, false)
        viewModel.useLiveProgress()

        downloadLargeFile.setOnClickListener { viewModel.downloadGcode(file, true) }
        retryButton.setOnClickListener { viewModel.downloadGcode(file, true) }
        downloadLargeFile.text = getString(R.string.download_x, file.size.asStyleFileSize())

        layerSeekBar.setOnSeekBarChangeListener(seekBarListener)
        layerProgressSeekBar.setOnSeekBarChangeListener(seekBarListener)

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.viewState.observe(viewLifecycleOwner, ::updateViewState)
        }
    }

    private fun updateViewState(state: GcodePreviewViewModel.ViewState) {
        TransitionManager.beginDelayedTransition(view as ViewGroup)

        renderGroup.isVisible = state is GcodePreviewViewModel.ViewState.DataReady
        largeFileGroup.isVisible = state is GcodePreviewViewModel.ViewState.LargeFileDownloadRequired
        errorGroup.isVisible = state is GcodePreviewViewModel.ViewState.Error

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
                Timber.i("Ready")

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
        }
    }

    private fun render(state: GcodePreviewViewModel.ViewState.DataReady) {
        if (state.renderContext == null || state.renderStyle == null || state.printerProfile == null) {
            Timber.e(IllegalStateException("Incomplete data ready state exposed ${state.renderContext == null} ${state.renderStyle == null} ${state.printerProfile == null}"))
            return
        }

        layerSeekBar.max = state.renderContext.layerCount - 1
        layerProgressSeekBar.max = LAYER_PROGRESS_STEPS
        if (state.fromUser != true) {
            layerSeekBar.progress = state.renderContext.layerNumber
            layerProgressSeekBar.progress = (state.renderContext.layerProgress * LAYER_PROGRESS_STEPS).roundToInt()
        }

        val layerHeightMm = DecimalFormat("0.0#").format(state.renderContext.layerZHeight)
        val layerProgressPercent = layerProgressSeekBar.progress / LAYER_PROGRESS_STEPS.toFloat()
        layerNumber.text = (layerSeekBar.progress + 1).toString()
        layerHeight.text = getString(R.string.x_mm, layerHeightMm)
        layerProgress.text = String.format("%.0f %%", layerProgressPercent * 100)

        renderView.renderParams = GcodeRenderView.RenderParams(
            renderContext = state.renderContext,
            renderStyle = state.renderStyle,
            printBedSizeMm = PointF(state.printerProfile.volume.width, state.printerProfile.volume.depth),
            extrusionWidthMm = state.printerProfile.extruder.nozzleDiameter,
        )
    }
}