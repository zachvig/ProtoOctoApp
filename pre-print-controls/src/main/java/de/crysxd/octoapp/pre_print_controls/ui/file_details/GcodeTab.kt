package de.crysxd.octoapp.pre_print_controls.ui.file_details

import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.gcode.parse.CuraGcodeParser
import de.crysxd.octoapp.base.gcode.render.GcodeRenderContextFactory
import de.crysxd.octoapp.base.gcode.render.GcodeRenderView
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.Injector
import de.crysxd.octoapp.pre_print_controls.di.injectParentViewModel
import kotlinx.android.synthetic.main.fragment_gcode_tab.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.system.measureTimeMillis

const val LAYER_PROGRESS_STEPS = 1000

class GcodeTab : Fragment(R.layout.fragment_gcode_tab) {

    private val viewModel: FileDetailsViewModel by injectParentViewModel(Injector.get().viewModelFactory())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
}