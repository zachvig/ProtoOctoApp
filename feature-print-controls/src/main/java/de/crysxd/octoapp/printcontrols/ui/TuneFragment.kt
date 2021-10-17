package de.crysxd.octoapp.printcontrols.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.common.OctoTextInputLayout
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.ext.clearFocusAndHideSoftKeyboard
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.octoapp.printcontrols.databinding.TuneFragmentBinding
import de.crysxd.octoapp.printcontrols.di.injectActivityViewModel
import de.crysxd.octoapp.printcontrols.di.injectViewModel
import de.crysxd.octoapp.printcontrols.ui.widget.TuneFragmentViewModel
import de.crysxd.octoapp.printcontrols.ui.widget.tune.TuneWidgetViewModel


class TuneFragment : BaseFragment() {

    override val viewModel: TuneFragmentViewModel by injectViewModel()
    private val tuneViewModel by injectActivityViewModel<TuneWidgetViewModel>()
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

        // Set values
        tuneViewModel.uiState.observe(viewLifecycleOwner) {
            if (!binding.feedRateInput.prepare().hasFocus()) {
                binding.feedRateInput.editText.setText(it.feedRate?.toString())
            }

            if (!binding.flowRateInput.prepare().hasFocus()) {
                binding.flowRateInput.editText.setText(it.flowRate?.toString())
            }

            if (!binding.fanSpeedInput.prepare().hasFocus()) {
                binding.fanSpeedInput.editText.setText(it.fanSpeed?.toString())
            }
        }

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