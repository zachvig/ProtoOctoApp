package de.crysxd.octoapp.print_controls.ui.widget.gcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.transition.TransitionManager
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.applyCanvas
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
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
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.octoprint.models.profiles.PrinterProfiles
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.databinding.GcodePreviewWidgetBinding
import de.crysxd.octoapp.print_controls.di.injectActivityViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

const val NOT_LIVE_IF_NO_UPDATE_FOR_MS = 5000L
private const val KEY_HIDDEN_AT = "gcode_preview_hidden_at"
private val HIDDEN_FOR_MILLIS = TimeUnit.DAYS.toMillis(30L)

class GcodePreviewWidget(context: Context) : RecyclableOctoWidget<GcodePreviewWidgetBinding, GcodePreviewViewModel>(context) {
    private var hideLiveIndicatorJob: Job? = null
    private lateinit var file: FileObject.File
    override val binding = GcodePreviewWidgetBinding.inflate(LayoutInflater.from(context))

    override fun createNewViewModel(parent: WidgetHostFragment) = parent.injectActivityViewModel<GcodePreviewViewModel>(Injector.get().viewModelFactory()).value

    override fun isVisible() = BillingManager.isFeatureEnabled("gcode_preview") ||
            (System.currentTimeMillis() - Injector.get().sharedPreferences().getLong(KEY_HIDDEN_AT, 0)) > HIDDEN_FOR_MILLIS

    override fun getTitle(context: Context) = context.getString(R.string.widget_gcode_preview)

    override fun getAnalyticsName() = "gcode_preview"

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        // We share the VM with the fullscreen. User might have used the sliders, return to live progress whenever we are started
        baseViewModel.useLiveProgress()
        baseViewModel.activeFile.observe(lifecycleOwner) {
            Timber.i("New file: $it")
            file = it
            baseViewModel.viewState.observe(lifecycleOwner, ::updateViewState)
            baseViewModel.downloadGcode(it, false)
        }
    }

    private fun updateViewState(state: GcodePreviewViewModel.ViewState) {
        TransitionManager.beginDelayedTransition(binding.root)
        binding.largeFileState.isVisible = false
        binding.errorState.isVisible = false
        binding.disabledState.isVisible = false
        binding.enabledState.isVisible = false

        when (state) {
            is GcodePreviewViewModel.ViewState.Loading -> {
                binding.loadingState.isVisible = true
                binding.progressBar.progress = (state.progress * 100).roundToInt()
                binding.progressBar.isIndeterminate = state.progress == 0f
                Timber.v("Progress: ${state.progress}")
            }

            is GcodePreviewViewModel.ViewState.FeatureDisabled -> {
                binding.loadingState.isVisible = false
                binding.disabledState.isVisible = true
                bindDisabledViewState(state.renderStyle)
                Timber.i("Feature disabled")
            }

            GcodePreviewViewModel.ViewState.LargeFileDownloadRequired -> {
                binding.loadingState.isVisible = false
                binding.largeFileState.isVisible = true
                bindLargeFileDownloadState()
                Timber.i("Large file download")
            }

            is GcodePreviewViewModel.ViewState.Error -> {
                binding.loadingState.isVisible = false
                binding.errorState.isVisible = true
                bindErrorState()
                Timber.i("Error")
            }

            is GcodePreviewViewModel.ViewState.DataReady -> {
                binding.loadingState.isVisible = false
                binding.enabledState.isVisible = true
                bindEnabledState(state)
                Timber.v("Data ready")
            }
        }
    }

    private fun bindErrorState() {
        binding.reloadButton.setOnClickListener {
            baseViewModel.downloadGcode(file, true)
        }
    }

    private fun bindLargeFileDownloadState() {
        binding.downloadLargeFile.text = parent.requireContext().getString(R.string.download_x, file.size.asStyleFileSize())
        binding.downloadLargeFile.setOnClickListener {
            baseViewModel.downloadGcode(file, true)
        }
    }

    private fun createPreviewImage(renderStyle: RenderStyle, attempt: Int = 0) {
        val width = binding.preview.width
        val height = binding.preview.height - binding.preview.paddingBottom - binding.preview.paddingTop

        // Ensure we are layed out. If not, try again (seems to happen sometimes)
        if (width <= 0 || height <= 0) {
            if (attempt < 3) {
                binding.disabledState.post {
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
        val background = ContextCompat.getDrawable(parent.requireContext(), renderStyle.background)
        val foreground = ContextCompat.getDrawable(parent.requireContext(), R.drawable.gcode_preview)
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
        binding.preview.setImageBitmap(bitmap)
    }

    private fun bindDisabledViewState(renderStyle: RenderStyle) {
        binding.disabledState.doOnNextLayout {
            createPreviewImage(renderStyle)
        }

        binding.buttonHide.setOnClickListener {
            OctoAnalytics.logEvent(OctoAnalytics.Event.DisabledFeatureHidden, bundleOf("feature" to "gcode_preview"))
            Injector.get().sharedPreferences().edit { putLong(KEY_HIDDEN_AT, System.currentTimeMillis()) }
            parent.reloadWidgets()
        }

        binding.buttonEnable.setOnClickListener {
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

        binding.liveIndicator.isVisible = true
        hideLiveIndicatorJob?.cancel()
        hideLiveIndicatorJob = parent.viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            delay(NOT_LIVE_IF_NO_UPDATE_FOR_MS)
            TransitionManager.beginDelayedTransition(binding.root)
            binding.liveIndicator.isVisible = false
        }

        binding.imageButtonFullscreen.setOnClickListener {
            try {
                it.findNavController().navigate(R.id.action_show_fullscreen_gcode, GcodePreviewFragmentArgs(file, true).toBundle())
                recordInteraction()
            } catch (e: Exception) {
                // Fix for https://bit.ly/39TmzR4 in lack of a better solution
                Timber.e(e)
            }
        }

        binding.layer.text =
            parent.requireContext().getString(de.crysxd.octoapp.base.R.string.x_of_y, renderContext.layerNumber + 1, renderContext.layerCount)
        binding.layerPorgess.text = String.format("%.0f %%", renderContext.layerProgress * 100)

        binding.renderView.isAcceptTouchInput = false
        binding.renderView.enableAsyncRender(parent.viewLifecycleOwner.lifecycleScope)
        binding.renderView.renderParams = GcodeRenderView.RenderParams(
            renderContext = state.renderContext!!,
            renderStyle = state.renderStyle!!,
            originInCenter = state.printerProfile?.volume?.origin == PrinterProfiles.Origin.Center,
            printBedSizeMm = PointF(printerProfile.volume.width, printerProfile.volume.depth),
            extrusionWidthMm = printerProfile.extruder.nozzleDiameter,
        )
    }
}