package de.crysxd.baseui.common.configureremote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import de.crysxd.baseui.databinding.ConfigureRemoteAccessSpaghettiDetectiveFragmentBinding
import de.crysxd.baseui.di.injectParentViewModel
import de.crysxd.octoapp.octoprint.isSpaghettiDetectiveUrl

class ConfigureRemoteAccessSpaghettiDetectiveFragment : Fragment() {

    private val viewModel by injectParentViewModel<ConfigureRemoteAccessViewModel>()
    private lateinit var binding: ConfigureRemoteAccessSpaghettiDetectiveFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ConfigureRemoteAccessSpaghettiDetectiveFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.connectTsd.setOnClickListener {
            viewModel.getSpaghettiDetectiveSetupUrl()
        }

        binding.disconnectTsd.setOnClickListener {
            viewModel.setRemoteUrl("", "", "", false)
        }

        viewModel.viewData.observe(viewLifecycleOwner) {
            val oeConnected = it.remoteWebUrl != null && it.remoteWebUrl.isSpaghettiDetectiveUrl()
            binding.tsdConnected.isVisible = oeConnected
            binding.disconnectTsd.isVisible = oeConnected
            binding.connectTsd.isVisible = !oeConnected
        }
    }
}