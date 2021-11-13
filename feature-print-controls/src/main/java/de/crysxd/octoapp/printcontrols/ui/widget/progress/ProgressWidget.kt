package de.crysxd.octoapp.printcontrols.ui.widget.progress

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ClipDrawable
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.INVISIBLE
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.crysxd.baseui.common.gcode.GcodePreviewViewModel
import de.crysxd.baseui.di.BaseUiInjector
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.baseui.utils.ColorTheme
import de.crysxd.baseui.widget.BaseWidgetHostFragment
import de.crysxd.baseui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.data.models.ProgressWidgetSettings
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.asPrintTimeLeftImageResource
import de.crysxd.octoapp.base.ext.asPrintTimeLeftOriginColor
import de.crysxd.octoapp.base.usecase.FormatDurationUseCase
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.printcontrols.R
import de.crysxd.octoapp.printcontrols.databinding.ProgressWidgetBinding
import de.crysxd.octoapp.printcontrols.di.injectActivityViewModel
import de.crysxd.octoapp.printcontrols.di.injectViewModel
import timber.log.Timber
import kotlin.math.roundToInt

class ProgressWidget(context: Context) : RecyclableOctoWidget<ProgressWidgetBinding, ProgressWidgetViewModel>(context) {
    override val type = WidgetType.ProgressWidget
    private val formatDurationUseCase: FormatDurationUseCase = BaseInjector.get().formatDurationUseCase()
    private val formatEtaUseCase = BaseInjector.get().formatEtaUseCase()
    private var lastProgress: Float? = null
    private var lastFile: String? = null
    private var lastSettings: ProgressWidgetSettings? = null
    override val binding = ProgressWidgetBinding.inflate(LayoutInflater.from(context))
    private val observer = Observer(::updateView)
    private val gcodeObserver = Observer(::updateLayer)
    private val progressPercentLayoutThreshold = 80f
    private var picasso: Picasso? = null
    private lateinit var gcodeViewModel: GcodePreviewViewModel

    override fun getActionIcon() = R.drawable.ic_round_settings_24

    override fun onAction() {
        MenuBottomSheetFragment.createForMenu(ProgressWidgetSettingsMenu()).show(parent.childFragmentManager)
    }

    override fun createNewViewModel(parent: BaseWidgetHostFragment): ProgressWidgetViewModel {
        val settings = BaseInjector.get().octoPreferences().progressWidgetSettings
        binding.eta.visibility = when (settings.etaStyle) {
            ProgressWidgetSettings.EtaStyle.None -> GONE
            ProgressWidgetSettings.EtaStyle.Compact -> INVISIBLE
            ProgressWidgetSettings.EtaStyle.Full -> INVISIBLE
        }
        binding.timeUsed.visibility = when (settings.showUsedTime) {
            false -> GONE
            true -> INVISIBLE
        }
        binding.timeLeft.visibility = when (settings.showLeftTime) {
            false -> GONE
            true -> INVISIBLE
        }
        binding.layer.visibility = when (settings.showLayer) {
            false -> GONE
            true -> INVISIBLE
        }
        binding.zHeight.visibility = when (settings.showZHeight) {
            false -> GONE
            true -> INVISIBLE
        }
        binding.printName.visibility = when (settings.printNameStyle) {
            ProgressWidgetSettings.PrintNameStyle.None -> GONE
            ProgressWidgetSettings.PrintNameStyle.Compact -> INVISIBLE
            ProgressWidgetSettings.PrintNameStyle.Full -> INVISIBLE
        }
        binding.preview.visibility = when (settings.showThumbnail) {
            false -> GONE
            true -> INVISIBLE
        }
        binding.textViewProgressPercent.isInvisible = true
        gcodeViewModel = parent.injectActivityViewModel<GcodePreviewViewModel>(BaseUiInjector.get().viewModelFactory()).value
        return parent.injectViewModel<ProgressWidgetViewModel>().value
    }

    override fun getTitle(context: Context) = context.getString(R.string.progress)
    override fun getAnalyticsName() = "progress"

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        baseViewModel.printState.observe(lifecycleOwner, observer)
        gcodeViewModel.viewState.observe(lifecycleOwner, gcodeObserver)
        BaseUiInjector.get().picasso().observe(lifecycleOwner) {
            picasso = it
        }
    }

    override fun onPause() {
        super.onPause()
        baseViewModel.printState.removeObserver(observer)
        gcodeViewModel.viewState.removeObserver(gcodeObserver)
    }

    private fun updateLayer(layerState: GcodePreviewViewModel.ViewState) {
        val (layer, zHeight) = when (layerState) {
            is GcodePreviewViewModel.ViewState.DataReady -> layerState.renderContext?.let {
                String.format("%d/%d (%.0f%%)", it.layerNumber, it.layerCount, it.layerProgress * 100) to context.getString(R.string.x_mm, it.layerZHeight)
            } ?: "Unavailable" to "Unavailable"
            is GcodePreviewViewModel.ViewState.Error -> "Unavailable" to "Unavailable"
            is GcodePreviewViewModel.ViewState.FeatureDisabled -> "Unavailable" to "Unavailable"
            GcodePreviewViewModel.ViewState.LargeFileDownloadRequired -> "Large file" to "Large File"
            is GcodePreviewViewModel.ViewState.Loading -> "Loading…" to "Loading…"
        }

        binding.layer.value = layer
        binding.zHeight.value = zHeight
    }

    private fun updateView(pair: Pair<Message.CurrentMessage, ProgressWidgetSettings>) {
        val (message, settings) = pair
        parent.lifecycleScope.launchWhenStarted {
            Timber.i("Received progress message ${message.copy(logs = emptyList(), temps = emptyList())}")
            val progressPercent = message.progress?.completion ?: 0f
            val progress = progressPercent.toInt() / 100f
            val printTimeLeft = message.progress?.printTimeLeft?.toLong()
            val printTimeSpent = message.progress?.printTime?.takeIf { it != 0 }?.toLong()
            val loading = progressPercent == 0f && printTimeLeft == null && printTimeSpent == null
            val formattedSpent = (printTimeSpent ?: 0L).takeIf { printTimeLeft != null }?.let { formatDuration(it) }
            val formattedLeft = printTimeLeft?.let { formatDuration(it) }
            val formattedEta = printTimeLeft?.let { formatEta(it, settings.etaStyle == ProgressWidgetSettings.EtaStyle.Compact) }
            val progressText = when {
                message.state?.flags?.cancelling == true -> context.getString(R.string.cancelling)
                loading -> context.getString(R.string.loading)
                else -> context.getString(R.string.x_percent, progress * 100f)
            }

            if (lastProgress != progress || lastSettings != settings) {
                TransitionManager.beginDelayedTransition(binding.root)
            }

            val p = picasso
            val file = message.job?.file
            if (lastFile != file?.path && file?.thumbnail != null && p != null && settings.showThumbnail) {
                Timber.i("Loading thumbnail: ${file.thumbnail}")
                lastFile = message.job?.file?.path
                p.load(file.thumbnail).into(binding.preview, object : Callback {
                    override fun onError(e: Exception?) = Unit
                    override fun onSuccess() {
                        TransitionManager.beginDelayedTransition(binding.root)
                        binding.preview.isVisible = true
                    }
                })
            }

            if (!settings.showThumbnail) {
                binding.preview.isVisible = false
                lastFile = null
            }

            updateProgressBar(progress, progressPercent)

            binding.textViewProgressPercent.setTextColor(
                ContextCompat.getColor(
                    parent.requireContext(), if (progressPercent > progressPercentLayoutThreshold) {
                        R.color.text_colored_background
                    } else {
                        R.color.normal_text
                    }
                )
            )

            (binding.progressBarFill.background as? ClipDrawable)?.apply {
                ObjectAnimator.ofInt(this, "level", level, (10000f * progress).roundToInt()).start()
            }

            if (loading) {
                return@launchWhenStarted
            }

            binding.printName.smallFont = settings.fontSize == ProgressWidgetSettings.FontSize.Small
            binding.eta.smallFont = binding.printName.smallFont
            binding.timeLeft.smallFont = binding.printName.smallFont
            binding.timeUsed.smallFont = binding.printName.smallFont
            binding.layer.smallFont = binding.printName.smallFont
            binding.zHeight.smallFont = binding.printName.smallFont
            val gap = context.resources.getDimensionPixelSize(if (binding.printName.smallFont) R.dimen.margin_1 else R.dimen.margin_2)
            binding.printName.updatePadding(top = gap / 2)
            binding.itemsFlow.setVerticalGap(gap / 2)

            binding.progressBarFill.backgroundTintList = ColorStateList.valueOf(ColorTheme.activeColorTheme.dark)
            binding.textViewProgressPercent.text = progressText
            binding.printName.value = message.job?.file?.display
            binding.printName.valueMaxLines = if (settings.printNameStyle == ProgressWidgetSettings.PrintNameStyle.Full) 10 else 1
            binding.eta.value = formattedEta
            binding.timeUsed.value = formattedSpent
            binding.timeLeft.value = formattedLeft
            binding.eta.labelIcon = ContextCompat.getDrawable(context, message.progress?.printTimeLeftOrigin.asPrintTimeLeftImageResource()).also {
                it?.setTint(ContextCompat.getColor(context, message.progress?.printTimeLeftOrigin.asPrintTimeLeftOriginColor()))
            }

            val hasLayerInfo = BillingManager.isFeatureEnabled(BillingManager.FEATURE_GCODE_PREVIEW)
            binding.eta.isVisible = settings.etaStyle != ProgressWidgetSettings.EtaStyle.None
            binding.timeUsed.isVisible = settings.showUsedTime
            binding.timeLeft.isVisible = settings.showLeftTime
            binding.zHeight.isVisible = settings.showZHeight && hasLayerInfo
            binding.layer.isVisible = settings.showLayer && hasLayerInfo
            binding.printName.isVisible = settings.printNameStyle != ProgressWidgetSettings.PrintNameStyle.None
            binding.textViewProgressPercent.isVisible = true

            lastSettings = settings
            lastProgress = progress
        }
    }

    private fun updateProgressBar(progress: Float, progressPercent: Float) {
        ConstraintSet().apply {
            clone(binding.root)
            constrainPercentWidth(R.id.progressBar, progress)
            clear(R.id.textViewProgressPercent, ConstraintSet.END)
            clear(R.id.textViewProgressPercent, ConstraintSet.START)
            connect(
                R.id.textViewProgressPercent,
                if (progressPercent > progressPercentLayoutThreshold) {
                    ConstraintSet.END
                } else {
                    ConstraintSet.START
                },
                R.id.progressBar,
                ConstraintSet.END
            )
            connect(
                R.id.textViewProgressPercent,
                if (progressPercent > progressPercentLayoutThreshold) {
                    ConstraintSet.START
                } else {
                    ConstraintSet.END
                },
                R.id.parent,
                ConstraintSet.END
            )
        }.applyTo(binding.root)

    }

    private suspend fun formatDuration(seconds: Long) = formatDurationUseCase.execute(seconds)
    private suspend fun formatEta(seconds: Long, compactDate: Boolean) =
        formatEtaUseCase.execute(FormatEtaUseCase.Params(seconds, allowRelative = false, showLabel = false, useCompactDate = compactDate))

}

