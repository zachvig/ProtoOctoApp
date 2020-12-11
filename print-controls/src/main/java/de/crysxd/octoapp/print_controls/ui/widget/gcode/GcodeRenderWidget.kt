package de.crysxd.octoapp.print_controls.ui.widget.gcode

import android.content.Context
import android.graphics.PointF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.render.GcodeRenderContextFactory
import de.crysxd.octoapp.base.gcode.render.GcodeRenderView
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.utils.measureTime
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.Injector
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.widget_gcode_render.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

class GcodeRenderWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: GcodeRenderWidgetViewModel by injectViewModel(Injector.get().viewModelFactory())

    override fun getTitle(context: Context) = "Gcode"

    override fun getAnalyticsName() = "gcode"

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.widget_gcode_render, container, false)

    override fun onViewCreated(view: View) {
        updateView(null)

        parent.viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val file = viewModel.file.first()
            viewModel.downloadGcode(file, false)
            view.downloadLargeFile.setOnClickListener { viewModel.downloadGcode(file, true) }
        }

        parent.viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.renderData.collectLatest {
                updateView(it)
            }
        }
    }

    private fun updateView(renderData: GcodeRenderWidgetViewModel.RenderData?) {
        TransitionManager.beginDelayedTransition(view as ViewGroup)

        view.progressOverlay.isVisible = renderData == null || renderData.gcode is GcodeFileDataSource.LoadState.Loading
        view.largeFileOverlay.isVisible = renderData?.gcode is GcodeFileDataSource.LoadState.FailedLargeFileDownloadRequired
        view.renderView.visibility = if (renderData?.gcode is GcodeFileDataSource.LoadState.Ready) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }

        renderData ?: return let {
            view.progressBar.isIndeterminate = true
        }

        when (renderData.gcode) {
            is GcodeFileDataSource.LoadState.Loading -> {
                view.progressBar.progress = (renderData.gcode.progress * 100).roundToInt()
                view.progressBar.isIndeterminate = renderData.gcode.progress == 0f
                Timber.v("Progress: ${renderData.gcode.progress}")
            }

            GcodeFileDataSource.LoadState.FailedLargeFileDownloadRequired -> Unit

            is GcodeFileDataSource.LoadState.Ready -> {
                render(renderData.gcode.gcode, renderData)
            }

            is GcodeFileDataSource.LoadState.Failed -> {
                parent.requireOctoActivity().showDialog(renderData.gcode.exception)
            }
        }
    }

    private fun render(gcode: Gcode, renderData: GcodeRenderWidgetViewModel.RenderData) = parent.viewLifecycleOwner.lifecycleScope.launchWhenCreated {
        val context = measureTime("Prepare context") {
            withContext(Dispatchers.Default) {
                GcodeRenderContextFactory.ForFileLocation(
                    byte = renderData.printInfo.printedBytes.toInt()
                ).extractMoves(gcode)
            }
        }

        measureTime("Render") {
            view.renderGroup.isVisible = true
            view.renderView.renderParams = GcodeRenderView.RenderParams(
                renderContext = context,
                renderStyle = renderData.renderStyle,
                printBedSizeMm = PointF(renderData.printerProfile.volume.width, renderData.printerProfile.volume.depth),
                extrusionWidthMm = renderData.printerProfile.extruder.nozzleDiameter,
            )
        }
    }
}