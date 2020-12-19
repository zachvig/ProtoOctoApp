package de.crysxd.octoapp.print_controls.ui.widget.progress

import android.animation.ObjectAnimator
import android.content.Context
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
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.base.ui.widget.progress.ProgressWidgetViewModel
import de.crysxd.octoapp.base.usecase.FormatDurationUseCase
import de.crysxd.octoapp.octoprint.models.socket.Message.CurrentMessage.ProgressInformation.Companion.ORIGIN_ANALYSIS
import de.crysxd.octoapp.octoprint.models.socket.Message.CurrentMessage.ProgressInformation.Companion.ORIGIN_AVERAGE
import de.crysxd.octoapp.octoprint.models.socket.Message.CurrentMessage.ProgressInformation.Companion.ORIGIN_ESTIMATE
import de.crysxd.octoapp.octoprint.models.socket.Message.CurrentMessage.ProgressInformation.Companion.ORIGIN_LINEAR
import de.crysxd.octoapp.octoprint.models.socket.Message.CurrentMessage.ProgressInformation.Companion.ORIGIN_MIXED_ANALYSIS
import de.crysxd.octoapp.octoprint.models.socket.Message.CurrentMessage.ProgressInformation.Companion.ORIGIN_MIXED_AVERAGE
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.widget_progress.view.*
import kotlin.math.roundToInt

class ProgressWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: ProgressWidgetViewModel by injectViewModel()
    private val formatDurationUseCase: FormatDurationUseCase = Injector.get().formatDurationUseCase()
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

                view.textViewProgressPercent.text = progressText
                view.textViewPrintName.text = it.job?.file?.display
                view.textViewTimeSpent.text = it.progress?.printTime?.toLong()?.let { formatDuration(it) }
                view.textViewTimeLeft.text = it.progress?.printTimeLeft?.toLong()?.let { formatDuration(it) }
                view.textVieEta.text = it.progress?.let { Injector.get().formatEtaUseCase().execute(it.printTimeLeft) }
                view.estimationIndicator.background?.setTint(
                    ContextCompat.getColor(
                        requireContext(),
                        when (it.progress?.printTimeLeftOrigin) {
                            ORIGIN_LINEAR -> R.color.analysis_bad
                            ORIGIN_ANALYSIS, ORIGIN_MIXED_ANALYSIS -> R.color.analysis_normal
                            ORIGIN_AVERAGE, ORIGIN_MIXED_AVERAGE, ORIGIN_ESTIMATE -> R.color.analysis_good
                            else -> android.R.color.transparent
                        }
                    )
                )

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

}