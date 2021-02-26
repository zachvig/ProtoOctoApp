package de.crysxd.octoapp.print_controls.ui.widget.gcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.applyCanvas
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.gcode.render.GcodeRenderView
import de.crysxd.octoapp.base.gcode.render.models.RenderStyle
import de.crysxd.octoapp.base.ui.common.gcode.GcodePreviewFragmentArgs
import de.crysxd.octoapp.base.ui.common.gcode.GcodePreviewViewModel
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectActivityViewModel
import de.crysxd.octoapp.print_controls.ui.PrintControlsFragment
import kotlinx.android.synthetic.main.widget_render_gcode.view.*
import kotlinx.android.synthetic.main.widget_render_gcode_disabled.view.*
import kotlinx.android.synthetic.main.widget_render_gcode_enabled.view.*
import kotlinx.android.synthetic.main.widget_render_gcode_error.view.*
import kotlinx.android.synthetic.main.widget_render_gcode_large_file.view.*
import kotlinx.android.synthetic.main.widget_render_gcode_loading.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

const val NOT_LIVE_IF_NO_UPDATE_FOR_MS = 5000L
private const val KEY_HIDDEN_AT = "gcode_preview_hidden_at"
private val HIDDEN_FOR_MILLIS = TimeUnit.DAYS.toMillis(30L)

class GcodePreviewWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: GcodePreviewViewModel by injectActivityViewModel(Injector.get().viewModelFactory())
    private var hideLiveIndicatorJob: Job? = null
    private lateinit var file: FileObject.File

    override fun isVisible() = BillingManager.isFeatureEnabled("gcode_preview") ||
            (System.currentTimeMillis() - Injector.get().sharedPreferences().getLong(KEY_HIDDEN_AT, 0)) > HIDDEN_FOR_MILLIS

    override fun getTitle(context: Context) = context.getString(R.string.widget_gcode_preview)

    override fun getAnalyticsName() = "gcode_preview"

    override fun onResume() {
        super.onResume()
        // We share the VM with the fullscreen. User might have used the sliders, return to live progress whenever we are started
        viewModel.useLiveProgress()
        viewModel.viewState.observe(parent.viewLifecycleOwner, ::updateViewState)
    }

    override fun onViewCreated(view: View) {
        viewModel.activeFile.observe(parent.viewLifecycleOwner) {
            Timber.i("New file: $it")
            file = it
            viewModel.downloadGcode(it, false)
        }
    }

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.widget_render_gcode, container, false)

    private fun updateViewState(state: GcodePreviewViewModel.ViewState) {
        TransitionManager.beginDelayedTransition(view as ViewGroup)
        view.largeFileState?.isVisible = false
        view.errorState?.isVisible = false
        view.disabledState?.isVisible = false
        view.enabledState?.isVisible = false

        when (state) {
            is GcodePreviewViewModel.ViewState.Loading -> {
                view.loadingStateStub?.isVisible = true
                view.loadingState?.isVisible = true
                view.progressBar?.progress = (state.progress * 100).roundToInt()
                view.progressBar?.isIndeterminate = state.progress == 0f
                Timber.v("Progress: ${state.progress}")
            }

            is GcodePreviewViewModel.ViewState.FeatureDisabled -> {
                view.loadingState?.isVisible = false
                view.disabledStateStub?.isVisible = true
                view.disabledState?.isVisible = true
                bindDisabledViewState(state.renderStyle)
                Timber.i("Feature disabled")
            }

            GcodePreviewViewModel.ViewState.LargeFileDownloadRequired -> {
                view.loadingState?.isVisible = false
                view.largeFileStateStub?.isVisible = true
                view.largeFileState?.isVisible = true
                bindLargeFileDownloadState()
                Timber.i("Large file download")
            }

            is GcodePreviewViewModel.ViewState.Error -> {
                view.loadingState?.isVisible = false
                view.errorStateStub?.isVisible = true
                view.errorState?.isVisible = true
                bindErrorState()
                Timber.i("Error")
            }

            is GcodePreviewViewModel.ViewState.DataReady -> {
                view.loadingState?.isVisible = false
                view.enabledStateStub?.isVisible = true
                view.enabledState?.isVisible = true
                bindEnabledState(state)
                Timber.v("Data ready")
            }
        }
    }

    private fun bindErrorState() {
        view.reloadButton?.setOnClickListener {
            viewModel.downloadGcode(file, true)
        }
    }

    private fun bindLargeFileDownloadState() {
        view.downloadLargeFile?.text = requireContext().getString(R.string.download_x, file.size.asStyleFileSize())
        view.downloadLargeFile?.setOnClickListener {
            viewModel.downloadGcode(file, true)
        }
    }

    private fun createPreviewImage(renderStyle: RenderStyle, attempt: Int = 0) {
        val width = view.preview.width
        val height = view.preview.height - view.preview.paddingBottom - view.preview.paddingTop

        // Ensure we are layed out. If not, try again (seems to happen sometimes)
        if (width <= 0 || height <= 0) {
            if (attempt < 3) {
                view.post {
                    createPreviewImage(renderStyle, attempt + 1)
                }
            }
            return
        }

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
        val background = ContextCompat.getDrawable(requireContext(), renderStyle.background)
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

    private fun bindDisabledViewState(renderStyle: RenderStyle) {
        view.doOnNextLayout {
            createPreviewImage(renderStyle)
        }

        view.buttonHide?.setOnClickListener {
            OctoAnalytics.logEvent(OctoAnalytics.Event.DisabledFeatureHidden, bundleOf("feature" to "gcode_preview"))
            Injector.get().sharedPreferences().edit { putLong(KEY_HIDDEN_AT, System.currentTimeMillis()) }
            view.findFragment<PrintControlsFragment>().reloadWidgets()
        }

        view.buttonEnable.setOnClickListener {
            OctoAnalytics.logEvent(OctoAnalytics.Event.PurchaseScreenOpen, bundleOf("trigger" to "gcode_live"))
            it.findNavController().navigate(R.id.action_show_purchase_flow)
        }
    }

    private fun bindEnabledState(state: GcodePreviewViewModel.ViewState.DataReady) {
        val renderContext = state.renderContext
        val renderStyle = state.renderStyle
        val printerProfile = state.printerProfile

        if (renderContext == null || renderStyle == null || printerProfile == null) {
            Timber.e(IllegalStateException("Incomplete data ready state exposed ${state.renderContext == null} ${state.renderStyle == null} ${state.printerProfile == null}"))
            return
        }

        view.liveIndicator.isVisible = true
        hideLiveIndicatorJob?.cancel()
        hideLiveIndicatorJob = parent.viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            delay(NOT_LIVE_IF_NO_UPDATE_FOR_MS)
            TransitionManager.beginDelayedTransition(view as ViewGroup)
            view.liveIndicator.isVisible = false
        }

        view.imageButtonFullscreen?.setOnClickListener {
            try {
                it.findNavController().navigate(R.id.action_show_fullscreen_gcode, GcodePreviewFragmentArgs(file, true).toBundle())
                recordInteraction()
            } catch (e: Exception) {
                // Fix for https://bit.ly/39TmzR4 in lack of a better solution
                Timber.e(e)
            }
        }

        view.layer.text = requireContext().getString(de.crysxd.octoapp.base.R.string.x_of_y, renderContext.layerNumber, renderContext.layerCount)
        view.layerPorgess.text = String.format("%.0f %%", renderContext.layerProgress * 100)

        view.renderView.isAcceptTouchInput = false
        view.renderView.enableAsyncRender(parent.viewLifecycleOwner.lifecycleScope)
        view.renderView.renderParams = GcodeRenderView.RenderParams(
            renderContext = state.renderContext!!,
            renderStyle = state.renderStyle!!,
            originInCenter = state.printerProfile?.volume?.origin == PrinterProfiles.Origin.Center,
            printBedSizeMm = PointF(printerProfile.volume.width, printerProfile.volume.depth),
            extrusionWidthMm = printerProfile.extruder.nozzleDiameter,
        )
    }
}