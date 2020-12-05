package de.crysxd.octoapp.pre_print_controls.ui.file_details

import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.render.GcodeRenderContextFactory
import de.crysxd.octoapp.base.gcode.render.GcodeRenderView
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.usecase.GenerateRenderStyleUseCase
import de.crysxd.octoapp.base.utils.measureTime
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.Injector
import de.crysxd.octoapp.pre_print_controls.di.injectParentViewModel
import kotlinx.android.synthetic.main.fragment_gcode_tab.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.DecimalFormat
import kotlin.math.roundToInt

const val LAYER_PROGRESS_STEPS = 1000

class GcodeTab : Fragment(R.layout.fragment_gcode_tab) {

    private val viewModel: FileDetailsViewModel by injectParentViewModel(Injector.get().viewModelFactory())
    private var previousRenderJob: Job? = null
    private var gcode: Gcode? = null
    private var profile: PrinterProfiles.Profile? = null
    private var renderStyle: RenderStyle? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.downloadGcode(false)

        downloadLargeFile.text = getString(R.string.download_x, viewModel.file.size.asStyleFileSize())
        downloadLargeFile.setOnClickListener { viewModel.downloadGcode(true) }

        // Prepare seekbars
        val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Show entire layer if layer is changed
                    if (seekBar == layerSeekBar) {
                        layerProgressSeekBar.progress = LAYER_PROGRESS_STEPS
                    }

                    render()
                }
            }
        }
        layerSeekBar.setOnSeekBarChangeListener(seekBarListener)
        layerProgressSeekBar.setOnSeekBarChangeListener(seekBarListener)

        var lastState: GcodeFileDataSource.LoadState? = null
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.gcodeDownloadFlow.collectLatest {
                try {
                    profile = it.second ?: profile
                    renderStyle = it.third
                    val downloadState = it.first

                    // Animate changes between states
                    val lastStateVal = lastState
                    if (lastStateVal == null || it::class.java != lastStateVal::class.java) {
                        lastState = downloadState
                        TransitionManager.beginDelayedTransition(view as ViewGroup)
                    }

                    layerSeekBar.isEnabled = false
                    layerProgressSeekBar.isEnabled = false
                    largeFileOverlay.isVisible = false

                    when (downloadState) {
                        is GcodeFileDataSource.LoadState.Loading -> {
                            progressOverlay.isVisible = true
                            progressBar.progress = (downloadState.progress * 100).roundToInt()
                            progressBar.isIndeterminate = downloadState.progress == 0f
                            Timber.v("Progress: ${downloadState.progress}")
                        }
                        is GcodeFileDataSource.LoadState.Ready -> {
                            progressOverlay.isVisible = false
                            gcode = downloadState.gcode
                            prepareGcodeRender(downloadState.gcode)
                            render()
                            Timber.i("Ready")

                        }
                        is GcodeFileDataSource.LoadState.Failed -> {
                            progressOverlay.isVisible = false
                            requireOctoActivity().showDialog(downloadState.exception)
                            Timber.i("Error :(")
                        }

                        is GcodeFileDataSource.LoadState.FailedLargeFileDownloadRequired -> {
                            largeFileOverlay.isVisible = true
                            Timber.i("Large download required")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    private fun prepareGcodeRender(gcode: Gcode) {
        layerSeekBar.isEnabled = true
        layerProgressSeekBar.isEnabled = true
        layerSeekBar.max = gcode.layers.size - 1
        layerProgressSeekBar.max = LAYER_PROGRESS_STEPS
        layerSeekBar.progress = gcode.layers.size
        layerProgressSeekBar.progress = LAYER_PROGRESS_STEPS
    }

    private fun render() {
        previousRenderJob?.cancel()
        previousRenderJob = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val gcode = gcode ?: return@launchWhenCreated
            val selectedLayer = layerSeekBar.progress
            val layerHeightMm = DecimalFormat("0.0#").format(gcode.layers[selectedLayer].zHeight)

            val layerProgressPercent = layerProgressSeekBar.progress / LAYER_PROGRESS_STEPS.toFloat()
            layerNumber.text = layerSeekBar.progress.toString()
            layerHeight.text = getString(R.string.x_mm, layerHeightMm)
            layerProgress.text = String.format("%.0f %%", layerProgressPercent * 100)

            measureTime("Prepare context") {
                val context = withContext(Dispatchers.Default) {
                    GcodeRenderContextFactory.ForLayerProgress(
                        layer = selectedLayer,
                        progress = layerProgressPercent
                    ).extractMoves(gcode)
                }

                TransitionManager.beginDelayedTransition(view as ViewGroup)
                profile?.let {
                    renderGroup.isVisible = true
                    renderView.renderParams = GcodeRenderView.RenderParams(
                        renderContext = context,
                        renderStyle = renderStyle ?: GenerateRenderStyleUseCase.defaultStyle,
                        printBedSizeMm = PointF(it.volume.width, it.volume.depth),
                        extrusionWidthMm = it.extruder.nozzleDiameter,
                    )
                }
            }.let {
                Timber.v("Preparing GcodeRenderView took ${it}ms")
            }
        }
    }
}