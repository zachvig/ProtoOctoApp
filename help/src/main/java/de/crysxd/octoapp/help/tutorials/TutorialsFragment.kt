package de.crysxd.octoapp.help.tutorials

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.help.databinding.TutorialFragmentBinding
import de.crysxd.octoapp.help.di.injectViewModel
import java.util.Date
import java.util.concurrent.TimeUnit

class TutorialsFragment : BaseFragment() {

    override val viewModel: TutorialsViewModel by injectViewModel()
    private lateinit var binding: TutorialFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        TutorialFragmentBinding.inflate(inflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TutorialsAdapter(requireContext()) {
            viewModel.createUri(it).open(requireActivity())
        }
        binding.list.adapter = adapter
        binding.buttonRetry.setOnClickListener { viewModel.reloadPlaylist() }
        viewModel.viewState.observe(viewLifecycleOwner) {
            TransitionManager.beginDelayedTransition(binding.root)
            binding.list.isVisible = it is TutorialsViewModel.ViewState.Data
            binding.loadingIndicator.isVisible = it is TutorialsViewModel.ViewState.Loading
            binding.error.isVisible = it is TutorialsViewModel.ViewState.Error

            if (it is TutorialsViewModel.ViewState.Data) {
                adapter.data = TutorialsAdapter.Data(
                    lastOpened = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)),
                    tutorials = it.videos
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
    }

    override fun onStop() {
        super.onStop()
        Injector.get().octoPreferences().tutorialsSeenAt = Date()
    }
}