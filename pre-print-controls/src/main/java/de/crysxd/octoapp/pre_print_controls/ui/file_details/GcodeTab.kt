package de.crysxd.octoapp.pre_print_controls.ui.file_details

import android.graphics.PointF
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.render.GcodeRenderContextFactory
import de.crysxd.octoapp.base.gcode.render.GcodeRenderView
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.utils.measureTime
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.Injector
import de.crysxd.octoapp.pre_print_controls.di.injectParentViewModel
import kotlinx.android.synthetic.main.fragment_gcode_tab.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

const val LAYER_PROGRESS_STEPS = 1000

class GcodeTab : Fragment(R.layout.fragment_gcode_tab) {

    private val viewModel: FileDetailsViewModel by injectParentViewModel(Injector.get().viewModelFactory())
    private var previousRenderJob: Job? = null
    private var gcode: Gcode? = null

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
                    // Animate changes between states
                    val lastStateVal = lastState
                    if (lastStateVal == null || it::class.java != lastStateVal::class.java) {
                        lastState = it
                        TransitionManager.beginDelayedTransition(view as ViewGroup)
                    }

                    layerSeekBar.isEnabled = false
                    layerProgressSeekBar.isEnabled = false
                    largeFileOverlay.isVisible = false

                    when (it) {
                        is GcodeFileDataSource.LoadState.Loading -> {
                            progressOverlay.isVisible = true
                            progressBar.progress = (it.progress * 100).roundToInt()
                            progressBar.isIndeterminate = it.progress == 0f
                            Timber.v("Progress: ${it.progress}")
                        }
                        is GcodeFileDataSource.LoadState.Ready -> {
                            progressOverlay.isVisible = false
                            gcode = it.gcode
                            prepareGcodeRender(it.gcode)
                            render()
                            Timber.i("Ready")

                        }
                        is GcodeFileDataSource.LoadState.Failed -> {
                            progressOverlay.isVisible = false
                            requireOctoActivity().showDialog(it.exception)
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
            progressBar.isVisible = true
            val gcode = gcode ?: return@launchWhenCreated

            val layerProgressPercent = layerProgressSeekBar.progress / LAYER_PROGRESS_STEPS.toFloat()
            layerNumber.text = layerSeekBar.progress.toString()
            layerProgress.text = String.format("%.0f %%", layerProgressPercent * 100)

            measureTime("Prepare context") {
                val context = withContext(Dispatchers.Default) {
                    GcodeRenderContextFactory.ForLayerProgress(
                        layer = layerSeekBar.progress,
                        progress = layerProgressPercent
                    ).extractMoves(gcode)
                }

                TransitionManager.beginDelayedTransition(view as ViewGroup)
                renderView.renderParams = GcodeRenderView.RenderParams(
                    renderContext = context,
                    printBedSizeMm = PointF(235f, 235f),
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.gcode_background_creality),
                    extrusionWidthMm = 0.5f,
                )

                progressBar.isVisible = false
            }.let {
                Timber.v("Preparing GcodeRenderView took ${it}ms")
            }
        }
    }
}