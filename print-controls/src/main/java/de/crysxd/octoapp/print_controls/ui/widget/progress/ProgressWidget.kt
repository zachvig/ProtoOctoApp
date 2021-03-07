package de.crysxd.octoapp.print_controls.ui.widget.progress

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ClipDrawable
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.asPrintTimeLeftOriginColor
import de.crysxd.octoapp.base.ui.ColorTheme
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.progress.ProgressWidgetViewModel
import de.crysxd.octoapp.base.usecase.FormatDurationUseCase
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.databinding.WidgetProgressBinding
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlin.math.roundToInt

class ProgressWidget(context: Context) : RecyclableOctoWidget<WidgetProgressBinding, ProgressWidgetViewModel>(context) {

    private val formatDurationUseCase: FormatDurationUseCase = Injector.get().formatDurationUseCase()
    private val formatEtaUseCase = Injector.get().formatEtaUseCase()
    private var lastProgress: Float? = null
    override val binding = WidgetProgressBinding.inflate(LayoutInflater.from(context))

    override fun createNewViewModel(parent: WidgetHostFragment) =
        parent.injectViewModel<ProgressWidgetViewModel>().value

    override fun getTitle(context: Context) = context.getString(R.string.progress)
    override fun getAnalyticsName() = "progress"

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        baseViewModel.printState.observe(lifecycleOwner, ::updateView)
    }

    private fun updateView(message: Message.CurrentMessage) {
        parent.lifecycleScope.launchWhenStarted {
            val progressPercent = message.progress?.completion ?: 0f
            val progressPercentLayoutThreshold = 80f
            val progress = progressPercent.toInt() / 100f
            val progressText = if (message.state?.flags?.cancelling == true) {
                context.getString(R.string.cancelling)
            } else {
                context.getString(R.string.x_percent, progress * 100f)
            }

            if (lastProgress != progress) {
                TransitionManager.beginDelayedTransition(binding.root)
            }

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

            binding.progressBarFill.backgroundTintList = ColorStateList.valueOf(ColorTheme.activeColorTheme.dark)
            binding.textViewProgressPercent.text = progressText
            binding.textViewPrintName.text = message.job?.file?.display
            binding.textViewTimeSpent.text = message.progress?.printTime?.toLong()?.let { formatDuration(it) }
            binding.textViewTimeLeft.text = message.progress?.printTimeLeft?.toLong()?.let { formatDuration(it) }
            binding.textVieEta.text = message.progress?.printTimeLeft?.toLong()?.let { formatEta(it) }
            binding.estimationIndicator.background?.setTint(ContextCompat.getColor(context, message.progress?.printTimeLeftOrigin.asPrintTimeLeftOriginColor()))
            binding.textViewProgressPercent.isVisible = true
            binding.textViewPrintName.isVisible = true
            binding.textViewTimeSpent.isVisible = true
            binding.textViewTimeLeft.isVisible = true
            binding.textVieEta.isVisible = true
            binding.estimationIndicator.isVisible = true

            lastProgress = progress
        }
    }

    private suspend fun formatDuration(seconds: Long) = formatDurationUseCase.execute(seconds)
    private suspend fun formatEta(seconds: Long) = formatEtaUseCase.execute(FormatEtaUseCase.Params(seconds, allowRelative = false, showLabel = false))

}