package de.crysxd.octoapp.print_controls.ui.widget.gcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.view.doOnNextLayout
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
import de.crysxd.octoapp.base.usecase.GenerateRenderStyleUseCase
import de.crysxd.octoapp.base.utils.measureTime
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.Injector
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.widget_render_gcode.view.*
import kotlinx.android.synthetic.main.widget_render_gcode_disabled.view.*
import kotlinx.android.synthetic.main.widget_render_gcode_enabled.view.*
import kotlinx.android.synthetic.main.widget_render_gcode_error.view.*
import kotlinx.android.synthetic.main.widget_render_gcode_large_file.view.*
import kotlinx.android.synthetic.main.widget_render_gcode_loading.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

const val NOT_LIVE_IF_NO_UPDATE_FOR_MS = 3000L

class GcodePreviewWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: GcodePreviewWidgetViewModel by injectViewModel(Injector.get().viewModelFactory())
    private var hideLiveIndicatorJob: Job? = null
    private lateinit var file: FileObject.File

    override fun getTitle(context: Context) = "Gcode"

    override fun getAnalyticsName() = "gcode"

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.widget_render_gcode, container, false)

    override fun onResume() {
        super.onResume()
        viewModel.renderData.observe(parent.viewLifecycleOwner, ::updateView)
        parent.viewLifecycleOwner.lifecycleScope.launchWhenResumed {

        }
    }

    override fun onViewCreated(view: View) {
        updateView(null)

        parent.viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            file = viewModel.file.first()
            viewModel.downloadGcode(file, false)
        }
    }

    private fun updateView(renderData: GcodePreviewWidgetViewModel.RenderData?) {
        TransitionManager.beginDelayedTransition(view as ViewGroup)

        val isDisabled = renderData?.featureEnabled == false
        view.disabledStateStub?.isVisible = true
        view.disabledState?.isVisible = true
        view.enabledState?.isVisible = false
        showPreview(renderData)

        val isLoading = renderData == null || renderData.gcode is GcodeFileDataSource.LoadState.Loading
        view.loadingStateStub?.isVisible = isLoading
        view.loadingState?.isVisible = isLoading

        val isLargeFile = renderData?.gcode is GcodeFileDataSource.LoadState.FailedLargeFileDownloadRequired
        view.largeFileStateStub?.isVisible = isLargeFile
        view.largeFileState?.isVisible = isLargeFile

        val isError = renderData?.gcode is GcodeFileDataSource.LoadState.Failed
        view.errorStateStub?.isVisible = isError
        view.errorState?.isVisible = isError

        view.reloadButton?.setOnClickListener {
            viewModel.downloadGcode(file, true)
        }

        view.imageButtonFullscreen?.setOnClickListener {
            it.findNavController().navigate(R.id.action_show_fullscreen_gcode, GcodePreviewFragmentArgs(file, true).toBundle())
        }

        view.downloadLargeFile?.text = requireContext().getString(R.string.download_x, file.size.asStyleFileSize())
        view.downloadLargeFile?.setOnClickListener {
            viewModel.downloadGcode(file, true)
        }

        renderData ?: return let {
            view.loadingStateStub?.isVisible = true
            view.loadingState?.isVisible = true
            view.progressBar?.isIndeterminate = true
        }

        when (renderData.gcode) {
            is GcodeFileDataSource.LoadState.Loading -> {
                view.progressBar?.progress = (renderData.gcode.progress * 100).roundToInt()
                view.progressBar?.isIndeterminate = renderData.gcode.progress == 0f
                Timber.v("Progress: ${renderData.gcode.progress}")
            }

            GcodeFileDataSource.LoadState.FailedLargeFileDownloadRequired -> Unit

            is GcodeFileDataSource.LoadState.Ready -> {
                view.enabledState?.isVisible = true
                view.disabledState?.isVisible = false
                render(renderData.gcode.gcode, renderData)
            }

            is GcodeFileDataSource.LoadState.Failed -> Unit
        }
    }

    private fun showPreview(renderData: GcodePreviewWidgetViewModel.RenderData?) {
        view.doOnNextLayout {
            val bitmap = Bitmap.createBitmap(
                view.preview.width,
                view.preview.height - view.preview.paddingBottom - view.preview.paddingTop,
                Bitmap.Config.ARGB_8888
            )
            val background = ContextCompat.getDrawable(requireContext(), renderData?.renderStyle?.background ?: GenerateRenderStyleUseCase.defaultStyle.background)
            val foreground = ContextCompat.getDrawable(requireContext(), R.drawable.gcode_preview)
            bitmap.applyCanvas {
                fun drawImage(image: Drawable?) = image?.let {
                    val backgroundScale = height / it.intrinsicHeight.toFloat()
                    it.setBounds(
                        (width - it.intrinsicWidth * backgroundScale).toInt(),
                        0,
                        width,
                        height
                    )
                    it.draw(this)
                }

                drawImage(background)
                drawImage(foreground)
            }
            view.preview.setImageBitmap(bitmap)
        }

        view.buttonHide.setOnClickListener {

        }

        view.buttonEnable.setOnClickListener {

        }
    }

    private fun render(gcode: Gcode, renderData: GcodePreviewWidgetViewModel.RenderData) = parent.viewLifecycleOwner.lifecycleScope.launchWhenCreated {
        renderData.printInfo ?: return@launchWhenCreated

        val context = measureTime("Prepare context") {
            withContext(Dispatchers.Default) {
                GcodeRenderContextFactory.ForFileLocation(
                    byte = renderData.printInfo.printedBytes.toInt()
                ).extractMoves(gcode)
            }
        }

        measureTime("Render") {
            view.layer.text = requireContext().getString(R.string.x_of_y, context.layerNumber, context.layerCount)
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