package de.crysxd.octoapp.printcontrols.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.common.OctoTextInputLayout
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.ext.clearFocusAndHideSoftKeyboard
import de.crysxd.baseui.ext.requestFocusAndOpenSoftKeyboard
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.octoapp.printcontrols.databinding.TuneFragmentBinding
import de.crysxd.octoapp.printcontrols.di.injectViewModel
import de.crysxd.octoapp.printcontrols.ui.widget.TuneFragmentViewModel

const val ARG_NO_VALUE = -1

class TuneFragment : BaseFragment() {

    override val viewModel: TuneFragmentViewModel by injectViewModel()
    private lateinit var binding: TuneFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        TuneFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show/Hide data hint
        viewModel.uiState.observe(viewLifecycleOwner) {
            if (!it.initialValue) {
                TransitionManager.beginDelayedTransition(view as ViewGroup)
            }

            if (it.initialValue && !binding.tutorial.isVisible) {
                binding.feedRateInput.editText.requestFocusAndOpenSoftKeyboard()
            }

            binding.buttonApply.isEnabled = !it.loading

            if (it.operationCompleted) {
                binding.feedRateInput.editText.clearFocusAndHideSoftKeyboard()
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
        binding.feedRateInput.prepare().editText.setText(if (args.currentFeedRate == ARG_NO_VALUE) null else args.currentFeedRate.toString())
        binding.flowRateInput.prepare().editText.setText(if (args.currentFlowRate == ARG_NO_VALUE) null else args.currentFlowRate.toString())
        binding.fanSpeedInput.prepare().editText.setText(if (args.currentFanSpeed == ARG_NO_VALUE) null else args.currentFanSpeed.toString())


        // Apply values
        binding.buttonApply.setOnClickListener { applyChanges() }
    }

    private fun applyChanges() {
        viewModel.applyChanges(
            feedRate = binding.feedRateInput.editText.text.toString().toIntOrNull(),
            flowRate = binding.flowRateInput.editText.text.toString().toIntOrNull(),
            fanSpeed = binding.fanSpeedInput.editText.text.toString().toIntOrNull()
        )
    }

    override fun onStart() {
        super.onStart()

        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Print
        requireOctoActivity().octo.isVisible = true
        binding.octoScrollView.setupWithToolbar(
            requireOctoActivity(),
            binding.buttonApplyContainer
        )
    }
}