package de.crysxd.octoapp.pre_print_controls.ui.file_details

import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.render.GcodeRenderContextFactory
import de.crysxd.octoapp.base.gcode.render.GcodeRenderView
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.Injector
import de.crysxd.octoapp.pre_print_controls.di.injectParentViewModel
import kotlinx.android.synthetic.main.fragment_gcode_tab.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.system.measureTimeMillis

const val LAYER_PROGRESS_STEPS = 1000

class GcodeTab : Fragment(R.layout.fragment_gcode_tab) {

    private val viewModel: FileDetailsViewModel by injectParentViewModel(Injector.get().viewModelFactory())
    private var previousRenderJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.downloadGcode()

        // Prepare seekbars
        val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = render()
        }
        layerSeekBar.setOnSeekBarChangeListener(seekBarListener)
        layerProgressSeekBar.setOnSeekBarChangeListener(seekBarListener)

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.gcodeDownloadFlow.collectLatest {
                layerSeekBar.isEnabled = false
                layerProgressSeekBar.isEnabled = false

                when (it) {
                    GcodeFileDataSource.LoadState.Loading -> {
                        progressBar.isVisible = true
                    }
                    is GcodeFileDataSource.LoadState.Ready -> {
                        progressBar.isVisible = false
                        prepareGcodeRender(it.gcode)
                        render()
                    }
                    is GcodeFileDataSource.LoadState.Failed -> {
                        progressBar.isVisible = false
                        requireOctoActivity().showDialog(it.exception)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val gcode = withContext(Dispatchers.IO) {
                File("/data/data/de.crysxd.octoapp/files/CE3_Green_box_engraved.gcode")
                    .readText().let { CuraGcodeParser().interpretFile(it) }
            }

            fun render() {
                viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                    val layerProgressPercent = layerProgressSeekBar.progress / LAYER_PROGRESS_STEPS.toFloat()
                    layerNumber.text = layerSeekBar.progress.toString()
                    layerProgress.text = String.format("%.0f %%", layerProgressPercent * 100)

                    measureTimeMillis {
                        val context = withContext(Dispatchers.Default) {
                            GcodeRenderContextFactory.ForLayerProgress(
                                layer = layerSeekBar.progress,
                                progress = layerProgressPercent
                            ).extractMoves(gcode)
                        }

                        renderView.renderParams = GcodeRenderView.RenderParams(
                            renderContext = context,
                            printBedSizeMm = PointF(235f, 235f),
                            background = ContextCompat.getDrawable(requireContext(), R.drawable.gcode_background_creality),
                            extrusionWidthMm = 0.5f,
                        )
                    }.let {
                        Timber.v("Preparing GcodeRenderView took ${it}ms")
                    }
                }
            }

            val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = render()
            }

            layerSeekBar.max = gcode.layers.size - 1
            layerProgressSeekBar.max = LAYER_PROGRESS_STEPS
            layerSeekBar.progress = gcode.layers.size
            layerProgressSeekBar.progress = LAYER_PROGRESS_STEPS

            layerSeekBar.setOnSeekBarChangeListener(seekBarListener)
            layerProgressSeekBar.setOnSeekBarChangeListener(seekBarListener)

            render()
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
            val currentGcode = (viewModel.gcodeDownloadFlow.firstOrNull() as? GcodeFileDataSource.LoadState.Ready)?.gcode
                ?: return@launchWhenCreated

            val layerProgressPercent = layerProgressSeekBar.progress / LAYER_PROGRESS_STEPS.toFloat()
            layerNumber.text = layerSeekBar.progress.toString()
            layerProgress.text = String.format("%.0f %%", layerProgressPercent * 100)

            measureTimeMillis {
                val context = withContext(Dispatchers.Default) {
                    GcodeRenderContextFactory.ForLayerProgress(
                        layer = layerSeekBar.progress,
                        progress = layerProgressPercent
                    ).extractMoves(currentGcode)
                }

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