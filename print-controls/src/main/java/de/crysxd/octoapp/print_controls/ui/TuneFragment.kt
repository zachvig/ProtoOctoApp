package de.crysxd.octoapp.print_controls.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.clearFocusAndHideSoftKeyboard
import de.crysxd.octoapp.base.ui.ext.requestFocusAndOpenSoftKeyboard
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.print_controls.R
import de.crysxd.octoapp.print_controls.di.Injector
import de.crysxd.octoapp.print_controls.di.injectViewModel
import de.crysxd.octoapp.print_controls.ui.widget.TuneFragmentViewModel
import kotlinx.android.synthetic.main.fragment_tune.*

const val ARG_NO_VALUE = -1

class TuneFragment : Fragment(R.layout.fragment_tune) {

    private val tuneFragmentViewModel: TuneFragmentViewModel by injectViewModel(Injector.get().viewModelFactory())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show/Hide data hint
        tuneFragmentViewModel.uiState.observe(viewLifecycleOwner, Observer {
            if (!it.initialValue) {
                TransitionManager.beginDelayedTransition(view as ViewGroup)
            }

            dataHint.isVisible = it.showDataHint

            if (it.initialValue && !it.showDataHint) {
                feedRateInput.editText.requestFocusAndOpenSoftKeyboard()
            }
        })

        // Hide data hint on click
        buttonHideHint.setOnClickListener {
            tuneFragmentViewModel.hideDataHint()
        }

        // Set initial values
        val args = navArgs<TuneFragmentArgs>().value
        feedRateInput.editText.setText(if (args.currentFeedRate == ARG_NO_VALUE) null else args.currentFeedRate.toString())
        flowRateInput.editText.setText(if (args.currentFlowRate == ARG_NO_VALUE) null else args.currentFlowRate.toString())
        fanSpeedInput.editText.setText(if (args.currentFanSpeed == ARG_NO_VALUE) null else args.currentFanSpeed.toString())

        // Apply values
        buttonApply.setOnClickListener {
            feedRateInput.editText.clearFocusAndHideSoftKeyboard()
            it.findNavController().popBackStack()
        }
    }

    override fun onStart() {
        super.onStart()

        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Print
        octoScrollView.setupWithToolbar(
            requireOctoActivity(),
            buttonApplyContainer
        )
    }
}