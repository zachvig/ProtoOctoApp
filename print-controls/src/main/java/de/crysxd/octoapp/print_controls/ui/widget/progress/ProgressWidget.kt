package de.crysxd.octoapp.print_controls.ui.widget.progress

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ClipDrawable
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.asPrintTimeLeftImageResource
import de.crysxd.octoapp.base.ext.asPrintTimeLeftOriginColor
import de.crysxd.octoapp.base.ui.ColorTheme
import de.crysxd.octoapp.base.ui.widget.BaseWidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.ui.widget.progress.ProgressWidgetViewModel
import de.crysxd.octoapp.base.usecase.FormatDurationUseCase
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.databinding.ProgressWidgetBinding
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class ProgressWidget(context: Context) : RecyclableOctoWidget<ProgressWidgetBinding, ProgressWidgetViewModel>(context) {

    private val formatDurationUseCase: FormatDurationUseCase = Injector.get().formatDurationUseCase()
    private val formatEtaUseCase = Injector.get().formatEtaUseCase()
    private var lastProgress: Float? = null
    override val binding = ProgressWidgetBinding.inflate(LayoutInflater.from(context))
    private val observer = Observer(::updateView)
    private var first = true
    private val progressPercentLayoutThreshold = 80f

    override fun createNewViewModel(parent: BaseWidgetHostFragment): ProgressWidgetViewModel {
        binding.textViewEtaLabel.isInvisible = true
        binding.textVieEta.isInvisible = true
        binding.estimationIndicator.isInvisible = true
        binding.textViewTimeSpentLabel.isInvisible = true
        binding.textViewTimeSpent.isInvisible = true
        binding.textViewTimeLeftLabel.isInvisible = true
        binding.textViewTimeLeft.isInvisible = true
        binding.textViewPrintName.isInvisible = true
        binding.textViewPrintNameLabel.isInvisible = true
        binding.textViewProgressPercent.isInvisible = true
        first = true
        return parent.injectViewModel<ProgressWidgetViewModel>().value
    }

    override fun getTitle(context: Context) = context.getString(R.string.progress)
    override fun getAnalyticsName() = "progress"

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        baseViewModel.printState.observe(lifecycleOwner, observer)
    }

    override fun onPause() {
        super.onPause()
        baseViewModel.printState.removeObserver(observer)
    }

    private fun updateView(message: Message.CurrentMessage) {
        parent.lifecycleScope.launchWhenStarted {
            val progressPercent = message.progress?.completion ?: 0f
            val progress = progressPercent.toInt() / 100f
            val printTimeLeft = message.progress?.printTimeLeft?.takeIf { it != 0 }?.toLong()
            val printTimeSpent = message.progress?.printTime?.takeIf { it != 0 }?.toLong()
            val loading = progressPercent == 0f && printTimeLeft == null && printTimeSpent == null
            val formattedSpent = (printTimeSpent ?: 0L).takeIf { printTimeLeft != null }?.let { formatDuration(it) }
            val formattedLeft = printTimeLeft?.let { formatDuration(it) }
            val formattedEta = printTimeLeft?.let { formatEta(it) }
            val progressText = when {
                message.state?.flags?.cancelling == true -> context.getString(R.string.cancelling)
                loading -> context.getString(R.string.loading)
                else -> context.getString(R.string.x_percent, progress * 100f)
            }

            if (first) {
                // Show intro animation :)
                delay(500)
                first = false
            }

            if (lastProgress != progress || first) {
                TransitionManager.beginDelayedTransition(binding.root)
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

            binding.progressBarFill.backgroundTintList = ColorStateList.valueOf(ColorTheme.activeColorTheme.dark)
            binding.textViewProgressPercent.text = progressText
            binding.textViewPrintName.text = message.job?.file?.display
            binding.textViewTimeSpent.text = formattedSpent
            binding.textViewTimeLeft.text = formattedLeft
            binding.textVieEta.text = formattedEta
            binding.estimationIndicator.isVisible = printTimeLeft != null
            binding.estimationIndicator.setImageResource(message.progress?.printTimeLeftOrigin.asPrintTimeLeftImageResource())
            binding.estimationIndicator.setColorFilter(ContextCompat.getColor(context, message.progress?.printTimeLeftOrigin.asPrintTimeLeftOriginColor()))
            binding.textViewEtaLabel.isVisible = true
            binding.textVieEta.isVisible = true
            binding.estimationIndicator.isVisible = true
            binding.textViewTimeSpentLabel.isVisible = true
            binding.textViewTimeSpent.isVisible = true
            binding.textViewPrintName.isVisible = true
            binding.textViewPrintNameLabel.isVisible = true
            binding.textViewProgressPercent.isVisible = true
            binding.textViewTimeLeftLabel.isVisible = true
            binding.textViewTimeLeft.isVisible = true
            lastProgress = progress
        }
    }

    private fun updateProgressBar(progress: Float, progressPercent: Float) {
        ConstraintSet().apply {
            clone(binding.root as ConstraintLayout)
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
    private suspend fun formatEta(seconds: Long) = formatEtaUseCase.execute(FormatEtaUseCase.Params(seconds, allowRelative = false, showLabel = false))

}