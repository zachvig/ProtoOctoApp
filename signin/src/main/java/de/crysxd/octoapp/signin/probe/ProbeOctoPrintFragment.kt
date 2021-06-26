package de.crysxd.octoapp.signin.probe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.signin.databinding.ProbeOctoprintFragmentBinding
import de.crysxd.octoapp.signin.di.injectViewModel
import timber.log.Timber

class ProbeOctoPrintFragment : BaseFragment() {
    override val viewModel by injectViewModel<ProbeOctoPrintViewModel>()
    private lateinit var binding: ProbeOctoprintFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ProbeOctoprintFragmentBinding.inflate(layoutInflater, container, false).also {
            binding = it
        }.root

    override fun onStart() {
        super.onStart()
        viewModel.probe(navArgs<ProbeOctoPrintFragmentArgs>().value.webUrl)
        viewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                is ProbeOctoPrintViewModel.UiState.FindingsReady -> Timber.i("${it.findings.size} findings")
                ProbeOctoPrintViewModel.UiState.Loading -> Timber.i("Loading")
            }
        }
    }
}