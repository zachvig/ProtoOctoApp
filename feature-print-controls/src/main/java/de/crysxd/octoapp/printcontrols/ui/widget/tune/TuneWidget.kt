package de.crysxd.octoapp.printcontrols.ui.widget.tune

import android.content.Context
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import de.crysxd.baseui.widget.BaseWidgetHostFragment
import de.crysxd.baseui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.printcontrols.R
import de.crysxd.octoapp.printcontrols.databinding.TuneWidgetBinding
import de.crysxd.octoapp.printcontrols.di.injectViewModel
import de.crysxd.octoapp.printcontrols.ui.ARG_NO_VALUE
import de.crysxd.octoapp.printcontrols.ui.PrintControlsFragmentDirections
import timber.log.Timber

class TuneWidget(context: Context) : RecyclableOctoWidget<TuneWidgetBinding, TuneWidgetViewModel>(context) {
    override val type = WidgetType.TuneWidget
    private val observer = Observer(::updateViewState)
    override val binding = TuneWidgetBinding.inflate(LayoutInflater.from(context))
    override fun createNewViewModel(parent: BaseWidgetHostFragment) = parent.injectViewModel<TuneWidgetViewModel>().value
    override fun getTitle(context: Context): String? = null
    override fun getAnalyticsName() = "tune"

    init {
        view.setOnClickListener {
            recordInteraction()
            baseViewModel.uiState.value?.let { uiState ->
                val exceptions = mutableListOf<Throwable>()

                fun NavController.launch() = try {
                    navigate(
                        PrintControlsFragmentDirections.actionTunePrint(
                            currentFanSpeed = uiState.fanSpeed ?: ARG_NO_VALUE,
                            currentFeedRate = uiState.feedRate ?: ARG_NO_VALUE,
                            currentFlowRate = uiState.flowRate ?: ARG_NO_VALUE
                        )
                    )
                } catch (e: Exception) {
                    exceptions.add(e)
                    null
                }


                it.findNavController().launch()?.let { Timber.i("DEBUG: Succeeded launching via it") }
                    ?: parent.findNavController().launch()?.let { Timber.i("DEBUG: Succeeded launching via parent") }

                Timber.i("DEBUG: parent.lifecycle=${parent.lifecycle.currentState} parent.viewLifecycle=${parent.viewLifecycleOwner.lifecycle.currentState}")
                exceptions.forEach { Timber.e(it) }
            }
        }
    }

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        baseViewModel.uiState.observe(lifecycleOwner, observer)
    }

    override fun onPause() {
        super.onPause()
        baseViewModel.uiState.removeObserver(observer)
    }

    private fun updateViewState(uiState: TuneWidgetViewModel.UiState) {
        TransitionManager.beginDelayedTransition(view as ViewGroup)

        binding.flowRate.isVisible = uiState.flowRate != null
        binding.textViewFlowRate.text = context.getString(R.string.x_percent_int, uiState.flowRate)

        binding.feedRate.isVisible = uiState.feedRate != null
        binding.textViewFeedRate.text = context.getString(R.string.x_percent_int, uiState.feedRate)

        binding.fanSpeed.isVisible = uiState.fanSpeed != null
        binding.textViewFanSpeed.text = context.getString(R.string.x_percent_int, uiState.fanSpeed)
    }
}