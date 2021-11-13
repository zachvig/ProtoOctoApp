package de.crysxd.octoapp.printcontrols.ui.widget.progress

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ClipDrawable
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.crysxd.baseui.di.BaseUiInjector
import de.crysxd.baseui.utils.ColorTheme
import de.crysxd.baseui.widget.BaseWidgetHostFragment
import de.crysxd.baseui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.asPrintTimeLeftImageResource
import de.crysxd.octoapp.base.ext.asPrintTimeLeftOriginColor
import de.crysxd.octoapp.base.usecase.FormatDurationUseCase
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import de.crysxd.octoapp.octoprint.models.socket.Message
import de.crysxd.octoapp.printcontrols.R
import de.crysxd.octoapp.printcontrols.databinding.ProgressWidgetBinding
import de.crysxd.octoapp.printcontrols.di.injectViewModel
import timber.log.Timber
import kotlin.math.roundToInt

class ProgressWidget(context: Context) : RecyclableOctoWidget<ProgressWidgetBinding, ProgressWidgetViewModel>(context) {
    override val type = WidgetType.ProgressWidget
    private val formatDurationUseCase: FormatDurationUseCase = BaseInjector.get().formatDurationUseCase()
    private val formatEtaUseCase = BaseInjector.get().formatEtaUseCase()
    private var lastProgress: Float? = null
    private var lastFile: String? = null
    override val binding = ProgressWidgetBinding.inflate(LayoutInflater.from(context))
    private val observer = Observer(::updateView)
    private val progressPercentLayoutThreshold = 80f
    private var picasso: Picasso? = null

    override fun createNewViewModel(parent: BaseWidgetHostFragment): ProgressWidgetViewModel {
        binding.eta.isInvisible = true
        binding.timeUsed.isInvisible = true
        binding.timeLeft.isInvisible = true
        binding.printName.isInvisible = true
        binding.preview.isVisible = false
        binding.textViewProgressPercent.isInvisible = true
        return parent.injectViewModel<ProgressWidgetViewModel>().value
    }

    override fun getTitle(context: Context) = context.getString(R.string.progress)
    override fun getAnalyticsName() = "progress"

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        baseViewModel.printState.observe(lifecycleOwner, observer)
        BaseUiInjector.get().picasso().observe(lifecycleOwner) {
            picasso = it
        }
    }

    override fun onPause() {
        super.onPause()
        baseViewModel.printState.removeObserver(observer)
    }

    private fun updateView(message: Message.CurrentMessage) {
        parent.lifecycleScope.launchWhenStarted {
            Timber.i("Received progress message ${message.copy(logs = emptyList(), temps = emptyList())}")
            val progressPercent = message.progress?.completion ?: 0f
            val progress = progressPercent.toInt() / 100f
            val printTimeLeft = message.progress?.printTimeLeft?.toLong()
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

            if (lastProgress != progress) {
                TransitionManager.beginDelayedTransition(binding.root)
            }

            val p = picasso
            val file = message.job?.file
            if (lastFile != file?.path && file?.thumbnail != null && p != null) {
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
            binding.printName.value = message.job?.file?.display
            binding.eta.value = formattedEta
            binding.timeUsed.value = formattedSpent
            binding.timeLeft.value = formattedLeft
            binding.eta.labelIcon = ContextCompat.getDrawable(context, message.progress?.printTimeLeftOrigin.asPrintTimeLeftImageResource()).also {
                it?.setTint(ContextCompat.getColor(context, message.progress?.printTimeLeftOrigin.asPrintTimeLeftOriginColor()))
            }

            binding.eta.isVisible = true
            binding.timeUsed.isVisible = true
            binding.timeLeft.isVisible = true
            binding.printName.isVisible = true
            binding.textViewProgressPercent.isVisible = true

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
    private suspend fun formatEta(seconds: Long) = formatEtaUseCase.execute(FormatEtaUseCase.Params(seconds, allowRelative = false, showLabel = false))

}