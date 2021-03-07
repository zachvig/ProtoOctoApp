package de.crysxd.octoapp.print_controls.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.ui.common.OctoTextInputLayout
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

    private val viewModel: TuneFragmentViewModel by injectViewModel(Injector.get().viewModelFactory())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show/Hide data hint
        viewModel.uiState.observe(viewLifecycleOwner) {
            if (!it.initialValue) {
                TransitionManager.beginDelayedTransition(view as ViewGroup)
            }

            if (it.initialValue && !tutorial.isVisible) {
                feedRateInput.editText.requestFocusAndOpenSoftKeyboard()
            }

            buttonApply.isEnabled = !it.loading

            if (it.operationCompleted) {
                feedRateInput.editText.clearFocusAndHideSoftKeyboard()
                findNavController().popBackStack()
            }
        }

        fun OctoTextInputLayout.prepare() = this.apply {
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.imeOptions = EditorInfo.IME_ACTION_DONE
            editText.setOnEditorActionListener { _, _, _ -> applyChanges(); true }
        }

        // Set initial values
        val args = navArgs<TuneFragmentArgs>().value
        feedRateInput.prepare().editText.setText(if (args.currentFeedRate == ARG_NO_VALUE) null else args.currentFeedRate.toString())
        flowRateInput.prepare().editText.setText(if (args.currentFlowRate == ARG_NO_VALUE) null else args.currentFlowRate.toString())
        fanSpeedInput.prepare().editText.setText(if (args.currentFanSpeed == ARG_NO_VALUE) null else args.currentFanSpeed.toString())


        // Apply values
        buttonApply.setOnClickListener { applyChanges() }
    }

    private fun applyChanges() {
        viewModel.applyChanges(
            feedRate = feedRateInput.editText.text.toString().toIntOrNull(),
            flowRate = flowRateInput.editText.text.toString().toIntOrNull(),
            fanSpeed = fanSpeedInput.editText.text.toString().toIntOrNull()
        )
    }

    override fun onStart() {
        super.onStart()

        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Print
        requireOctoActivity().octo.isVisible = true
        octoScrollView.setupWithToolbar(
            requireOctoActivity(),
            buttonApplyContainer
        )
    }
}