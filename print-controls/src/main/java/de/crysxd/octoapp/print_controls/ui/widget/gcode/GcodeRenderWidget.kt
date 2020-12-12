package de.crysxd.octoapp.print_controls.ui.widget.gcode

import android.content.Context
import android.graphics.PointF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.datasource.GcodeFileDataSource
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.gcode.parse.models.Gcode
import de.crysxd.octoapp.base.gcode.render.GcodeRenderContextFactory
import de.crysxd.octoapp.base.gcode.render.GcodeRenderView
import de.crysxd.octoapp.base.ui.common.gcode.GcodePreviewFragmentArgs
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.utils.measureTime
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.Injector
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.widget_render_gcode.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

const val NOT_LIVE_IF_NO_UPDATE_FOR_MS = 3000L

class GcodeRenderWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: GcodeRenderWidgetViewModel by injectViewModel(Injector.get().viewModelFactory())
    private var hideLiveIndicatorJob: Job? = null

    override fun getTitle(context: Context) = "Gcode"

    override fun getAnalyticsName() = "gcode"

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.widget_render_gcode, container, false)

    override fun onResume() {
        super.onResume()
        parent.viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.renderData.collectLatest {
                updateView(it)
            }
        }
    }

    override fun onViewCreated(view: View) {
        updateView(null)


        parent.viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            val file = viewModel.file.first()

            viewModel.downloadGcode(file, false)

            view.reloadButton.setOnClickListener {
                viewModel.downloadGcode(file, true)
            }

            view.imageButtonFullscreen.setOnClickListener {
                it.findNavController().navigate(R.id.action_show_fullscreen_gcode, GcodePreviewFragmentArgs(file, true).toBundle())
            }

            view.downloadLargeFile.text = requireContext().getString(R.string.download_x, file.size.asStyleFileSize())
            view.downloadLargeFile.setOnClickListener {
                viewModel.downloadGcode(file, true)
                updateView(
                    GcodeRenderWidgetViewModel.RenderData(gcode = GcodeFileDataSource.LoadState.Loading(0f))
                )
            }
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
        view.renderGroup.isVisible = renderData?.gcode is GcodeFileDataSource.LoadState.Ready
        view.errorGroup.isVisible = renderData?.gcode is GcodeFileDataSource.LoadState.Failed

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

            is GcodeFileDataSource.LoadState.Failed -> Unit
        }
    }

    private fun render(gcode: Gcode, renderData: GcodeRenderWidgetViewModel.RenderData) = parent.viewLifecycleOwner.lifecycleScope.launchWhenCreated {
        renderData.printInfo ?: return@launchWhenCreated

        val context = measureTime("Prepare context") {
            withContext(Dispatchers.Default) {
                GcodeRenderContextFactory.ForFileLocation(
                    byte = renderData.printInfo.printedBytes.toInt()
                ).extractMoves(gcode)
            }
        }

        measureTime("Render") {
            view.renderGroup.isVisible = true
            view.layer.text = context.layerNumber.toString()
            view.layerPorgess.text = requireContext().getString(R.string.x_percent_int, (context.layerProgress * 100).toInt())
            view.liveIndicator.isVisible = true
            hideLiveIndicatorJob?.cancel()
            hideLiveIndicatorJob = parent.viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                delay(NOT_LIVE_IF_NO_UPDATE_FOR_MS)
                view.liveIndicator.isVisible = false
            }

            renderData.renderStyle ?: return@launchWhenCreated
            renderData.printerProfile ?: return@launchWhenCreated

            view.renderView.renderParams = GcodeRenderView.RenderParams(
                renderContext = context,
                renderStyle = renderData.renderStyle,
                printBedSizeMm = PointF(renderData.printerProfile.volume.width, renderData.printerProfile.volume.depth),
                extrusionWidthMm = renderData.printerProfile.extruder.nozzleDiameter,
            )
        }
    }
}