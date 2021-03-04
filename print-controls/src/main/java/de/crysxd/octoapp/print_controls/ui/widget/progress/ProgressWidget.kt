package de.crysxd.octoapp.print_controls.ui.widget.progress

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ClipDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.asPrintTimeLeftOriginColor
import de.crysxd.octoapp.base.ui.ColorTheme
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.progress.ProgressWidgetViewModel
import de.crysxd.octoapp.base.usecase.FormatDurationUseCase
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.widget_progress.view.*
import kotlin.math.roundToInt

class ProgressWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: ProgressWidgetViewModel by injectViewModel()
    private val formatDurationUseCase: FormatDurationUseCase = Injector.get().formatDurationUseCase()
    private val formatEtaUseCase = Injector.get().formatEtaUseCase()
    private var lastProgress: Float? = null

    override fun getTitle(context: Context) = context.getString(R.string.progress)
    override fun getAnalyticsName() = "progress"

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.suspendedInflate(R.layout.widget_progress, container, false)

    override fun onViewCreated(view: View) {
        viewModel.printState.observe(parent, {
            parent.lifecycleScope.launchWhenStarted {
                val progressPercent = it.progress?.completion ?: 0f
                val progressPercentLayoutThreshold = 80f
                val progress = progressPercent.toInt() / 100f
                val progressText = if (it.state?.flags?.cancelling == true) {
                    requireContext().getString(R.string.cancelling)
                } else {
                    requireContext().getString(R.string.x_percent, progress * 100f)
                }

                if (lastProgress != progress) {
                    TransitionManager.beginDelayedTransition(view as ViewGroup, InstantAutoTransition())
                }

                ConstraintSet().apply {
                    clone(view as ConstraintLayout)
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
                }.applyTo(view as ConstraintLayout)

                view.textViewProgressPercent.setTextColor(
                    ContextCompat.getColor(
                        requireContext(), if (progressPercent > progressPercentLayoutThreshold) {
                            R.color.text_colored_background
                        } else {
                            R.color.normal_text
                        }
                    )
                )

                (view.progressBarFill.background as? ClipDrawable)?.apply {
                    ObjectAnimator.ofInt(this, "level", level, (10000f * progress).roundToInt()).start()
                }

                view.progressBarFill.backgroundTintList = ColorStateList.valueOf(ColorTheme.activeColorTheme.dark)
                view.textViewProgressPercent.text = progressText
                view.textViewPrintName.text = it.job?.file?.display
                view.textViewTimeSpent.text = it.progress?.printTime?.toLong()?.let { formatDuration(it) }
                view.textViewTimeLeft.text = it.progress?.printTimeLeft?.toLong()?.let { formatDuration(it) }
                view.textVieEta.text = it.progress?.printTimeLeft?.toLong()?.let { formatEta(it) }
                view.estimationIndicator.background?.setTint(ContextCompat.getColor(requireContext(), it.progress?.printTimeLeftOrigin.asPrintTimeLeftOriginColor()))
                view.textViewProgressPercent.isVisible = true
                view.textViewPrintName.isVisible = true
                view.textViewTimeSpent.isVisible = true
                view.textViewTimeLeft.isVisible = true
                view.textVieEta.isVisible = true
                view.estimationIndicator.isVisible = true

                lastProgress = progress
            }
        })
    }

    private suspend fun formatDuration(seconds: Long) = formatDurationUseCase.execute(seconds)
    private suspend fun formatEta(seconds: Long) = formatEtaUseCase.execute(FormatEtaUseCase.Params(seconds, allowRelative = false, showLabel = false))

}