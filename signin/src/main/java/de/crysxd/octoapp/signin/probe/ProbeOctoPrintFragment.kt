package de.crysxd.octoapp.signin.probe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.signin.databinding.BaseSigninFragmentBinding
import de.crysxd.octoapp.signin.di.injectViewModel
import timber.log.Timber

class ProbeOctoPrintFragment : BaseFragment() {
    override val viewModel by injectViewModel<ProbeOctoPrintViewModel>()
    private lateinit var binding: BaseSigninFragmentBinding
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(Injector.get().viewModelFactory())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        BaseSigninFragmentBinding.inflate(layoutInflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loading.subtitle.isVisible = false
        binding.loading.title.text = "Testing the connection to OctoPrint..."

        wifiViewModel.networkState.observe(viewLifecycleOwner) {
            Timber.i("Wifi state: $it")
            binding.wifiWarning.isVisible = it is NetworkStateViewModel.NetworkState.WifiNotConnected
        }

        viewModel.probe(navArgs<ProbeOctoPrintFragmentArgs>().value.webUrl)
        viewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                is ProbeOctoPrintViewModel.UiState.FindingsReady -> it.finding?.let { Timber.i("Found: $it") } ?: Timber.i("No findings")
                ProbeOctoPrintViewModel.UiState.Loading -> Timber.i("Loading")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
    }
}