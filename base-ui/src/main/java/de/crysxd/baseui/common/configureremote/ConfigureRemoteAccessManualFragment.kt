package de.crysxd.baseui.common.configureremote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import de.crysxd.baseui.R
import de.crysxd.baseui.databinding.ConfigureRemoteAccessManualFragmentBinding
import de.crysxd.baseui.di.injectParentViewModel
import de.crysxd.octoapp.octoprint.extractAndRemoveBasicAuth
import de.crysxd.octoapp.octoprint.isOctoEverywhereUrl

class ConfigureRemoteAccessManualFragment : Fragment() {
    private val viewModel by injectParentViewModel<ConfigureRemoteAccessViewModel>()
    private lateinit var binding: ConfigureRemoteAccessManualFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ConfigureRemoteAccessManualFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveUrl.setOnClickListener {
            viewModel.setRemoteUrl(
                url = binding.webUrlInput.editText.text.toString(),
                username = binding.basicUserInput.editText.text.toString(),
                password = binding.basicPasswordInput.editText.text.toString(),
                bypassChecks = false
            )
        }

        val inputTint = ContextCompat.getColor(requireContext(), R.color.input_background_alternative)
        binding.webUrlInput.backgroundTint = inputTint
        binding.basicPasswordInput.backgroundTint = inputTint
        binding.basicUserInput.backgroundTint = inputTint

        viewModel.viewState.observe(viewLifecycleOwner) {
            binding.saveUrl.isEnabled = it !is ConfigureRemoteAccessViewModel.ViewState.Loading
            binding.saveUrl.setText(if (binding.saveUrl.isEnabled) R.string.configure_remote_acces___manual___button else R.string.loading)
        }

        viewModel.viewData.observe(viewLifecycleOwner) {
            val manualConnected = it.remoteWebUrl != null && !it.remoteWebUrl.isOctoEverywhereUrl()
            binding.webUrlInput.editText.setText(it.remoteWebUrl?.extractAndRemoveBasicAuth()?.first.takeIf { manualConnected }?.toString())
            binding.basicPasswordInput.editText.setText(it.remoteWebUrl?.password?.takeIf { manualConnected }?.toString())
            binding.basicUserInput.editText.setText(it.remoteWebUrl?.username?.takeIf { manualConnected }?.toString())
        }

        viewModel.viewEvents.observe(viewLifecycleOwner) {
            if (it is ConfigureRemoteAccessViewModel.ViewEvent.Success) {
                binding.webUrlInput.editText.clearFocus()
                binding.basicPasswordInput.editText.clearFocus()
                binding.basicUserInput.editText.clearFocus()
            }
        }
    }
}