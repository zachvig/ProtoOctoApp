package de.crysxd.octoapp.signin.manual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.ManualWebUrlFragmentBinding
import de.crysxd.octoapp.signin.di.injectViewModel

class ManualWebUrlFragment : BaseFragment() {
    override val viewModel by injectViewModel<ManualWebUrlViewModel>()
    private lateinit var binding: ManualWebUrlFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ManualWebUrlFragmentBinding.inflate(layoutInflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonContinue.setOnClickListener {
            viewModel.testWebUrl(binding.input.editText.text?.toString() ?: "")
        }
        var failureCounter = 0
        viewModel.viewState.observe(viewLifecycleOwner) {
            when (it) {
                is ManualWebUrlViewModel.ViewState.Error -> if (!it.handled) {
                    it.handled = true
                    failureCounter++
                    if (!it.message.isNullOrBlank()) {
                        requireOctoActivity().showDialog(
                            message = it.message,
                            neutralButton = getString(R.string.show_details),
                            neutralAction = { _ ->
                                requireOctoActivity().showErrorDetailsDialog(it.exception, offerSupport = failureCounter > 1)
                            }
                        )
                    } else {
                        requireOctoActivity().showErrorDetailsDialog(it.exception, offerSupport = failureCounter > 1)
                    }
                }

                is ManualWebUrlViewModel.ViewState.Success -> {
                    Toast.makeText(requireContext(), "continue with validation", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.input.showSoftKeyboard()
        requireOctoActivity().octo.isVisible = false
    }

    override fun onStop() {
        super.onStop()
        binding.input.hideSoftKeyboard()
    }

}