package de.crysxd.baseui.common.configureremote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import de.crysxd.baseui.databinding.ConfigureRemoteAccessOctoeverywhereFragmentBinding
import de.crysxd.baseui.di.injectParentViewModel
import de.crysxd.octoapp.octoprint.isOctoEverywhereUrl

class ConfigureRemoteAccessOctoEverywhereFragment : Fragment() {

    private val viewModel by injectParentViewModel<ConfigureRemoteAccessViewModel>()
    private lateinit var binding: ConfigureRemoteAccessOctoeverywhereFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ConfigureRemoteAccessOctoeverywhereFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.connectOctoEverywhere.setOnClickListener {
            viewModel.getOctoEverywhereAppPortalUrl()
        }

        binding.disconnectOctoEverywhere.setOnClickListener {
            viewModel.setRemoteUrl("", "", "", false)
        }

        viewModel.viewData.observe(viewLifecycleOwner) {
            val oeConnected = it.remoteWebUrl != null && it.remoteWebUrl.isOctoEverywhereUrl()
            binding.octoEverywhereConnected.isVisible = oeConnected
            binding.disconnectOctoEverywhere.isVisible = oeConnected
            binding.connectOctoEverywhere.isVisible = !oeConnected
            binding.description1.isVisible = !oeConnected
            binding.description2.isVisible = !oeConnected
        }
    }
}