package de.crysxd.octoapp.signin.discover

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.DiscoverFragmentInitialBinding
import de.crysxd.octoapp.signin.di.injectViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.floor
import de.crysxd.octoapp.base.di.Injector as BaseInjector

class DiscoverFragment : BaseFragment() {
    override val viewModel by injectViewModel<DiscoverViewModel>()
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(BaseInjector.get().viewModelFactory())
    private lateinit var binding: DiscoverFragmentInitialBinding
    private var continueManuallyJob: Job? = null
    var viewMovedToSecondaryLayout = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        DiscoverFragmentInitialBinding.inflate(inflater, container, false).also {
            binding = it
            viewMovedToSecondaryLayout = false
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scheduleContinueManually()
        binding.content.helpOption.showHelp()
        binding.content.helpOption.setOnClickListener { }
        binding.content.manualConnectOption.showManualConnect()
        binding.content.manualConnectOption.setOnClickListener { continueWithManualConnect() }
        binding.content.buttonDelete.setOnClickListener {
            beginDelayedTransition()
            binding.content.buttonDelete.isVisible = false
            binding.content.previousOptions.forEach {
                (it as? DiscoverOptionView)?.showDelete()
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            if (!viewMovedToSecondaryLayout && (it.connectedOctoPrint.isNotEmpty() || it.discoveredOctoPrint.isNotEmpty())) {
                Timber.i("Content arrived, moving view state")
                moveToSecondaryLayout()
                continueManuallyJob?.cancel()
            }

            createDiscoveredOptions(it.discoveredOctoPrint)
            createPreviouslyConnectedOptions(it.connectedOctoPrint)
        }


        wifiViewModel.networkState.observe(viewLifecycleOwner) {
            binding.wifiWarning.isVisible = it is NetworkStateViewModel.NetworkState.WifiNotConnected
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
    }

    private fun beginDelayedTransition() = TransitionManager.beginDelayedTransition(binding.root, InstantAutoTransition())

    private fun scheduleContinueManually() {
        // Delay the manual button a bit so we hopefully automatically find OctoPrint
        continueManuallyJob = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val delay = DiscoverViewModel.INITIAL_LOADING_DELAY_MS + 500
            val steps = 100
            val stepDelay = floor(delay / steps.toFloat()).toLong() / 2
            binding.progressBar.max = steps
            repeat(steps) {
                delay(stepDelay)
                binding.progressBar.progress = it + 1
            }
            binding.progressBar.isIndeterminate = true
            continueWithManualConnect()
        }
    }

    private fun createDiscoveredOptions(options: List<DiscoverOctoPrintUseCase.DiscoveredOctoPrint>) {
        beginDelayedTransition()

        // Delete all that are not longer shown
        binding.content.previousOptions.forEach {
            if (it !is DiscoverOptionView) binding.content.discoveredOptions.removeView(it)
            else if (!options.any { o -> it.isShowing(o) }) binding.content.discoveredOptions.removeView(it)
        }

        // Add all missing
        options.map {
            binding.content.discoveredOptions.children.firstOrNull { view ->
                view is DiscoverOptionView && view.isShowing(it)
            } ?: DiscoverOptionView(requireContext()).apply {
                show(it)
                setOnClickListener { _ -> continueWithDiscovered(it) }
            }
        }.filter {
            it.parent == null
        }.forEach {
            binding.content.discoveredOptions.addView(it, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        // Update title visibility
        binding.content.discoveredOptionsTitle.isVisible = binding.content.discoveredOptions.childCount > 0
        binding.content.discoveredHelp.isVisible = binding.content.discoveredOptionsTitle.isVisible
    }

    private fun createPreviouslyConnectedOptions(options: List<OctoPrintInstanceInformationV2>) {
        beginDelayedTransition()

        // Delete all that are not longer shown
        binding.content.previousOptions.forEach {
            if (it !is DiscoverOptionView) binding.content.previousOptions.removeView(it)
            else if (!options.any { o -> it.isShowing(o) }) binding.content.previousOptions.removeView(it)
        }

        // Add all missing
        options.map {
            binding.content.previousOptions.children.firstOrNull { view ->
                view is DiscoverOptionView && view.isShowing(it)
            } ?: DiscoverOptionView(requireContext()).apply {
                show(it)
                setOnClickListener { _ -> continueWithPreviouslyConnected(it) }
                onDelete = { deleteAfterConfirmation(it) }
            }
        }.filter {
            it.parent == null
        }.forEach {
            binding.content.previousOptions.addView(it, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        // Update title visibility
        binding.content.previousOptionsTitle.isVisible = binding.content.previousOptions.childCount > 0
        binding.content.buttonDelete.isVisible = binding.content.buttonDelete.isVisible && binding.content.previousOptionsTitle.isVisible
    }

    private fun continueWithDiscovered(octoPrint: DiscoverOctoPrintUseCase.DiscoveredOctoPrint) {
        Toast.makeText(requireContext(), octoPrint.label, Toast.LENGTH_SHORT).show()
    }

    private fun continueWithPreviouslyConnected(octoPrint: OctoPrintInstanceInformationV2) {
        Toast.makeText(requireContext(), octoPrint.label, Toast.LENGTH_SHORT).show()
    }

    private fun continueWithManualConnect() {
        Toast.makeText(requireContext(), "Connect manually", Toast.LENGTH_SHORT).show()
    }

    private fun deleteAfterConfirmation(option: OctoPrintInstanceInformationV2) {
        requireOctoActivity().showDialog(
            message = "Delete ${option.label} and all associated settings?",
            positiveButton = "Delete",
            positiveAction = { viewModel.deleteInstance(option.webUrl) },
            neutralAction = {},
            neutralButton = getString(R.string.cancel)
        )
    }

    private fun moveToSecondaryLayout() {
        beginDelayedTransition()
        viewMovedToSecondaryLayout = true
        val wifiWasVisible = binding.wifiWarning.isVisible
        ConstraintSet().let {
            it.load(requireContext(), R.layout.discover_fragment)
            it.applyTo(binding.root)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.title.setTextAppearance(R.style.OctoTheme_TextAppearance_Title)
            binding.subtitle.setTextAppearance(R.style.OctoTheme_TextAppearance)
        }
        binding.subtitle.text = "Select a option below"
        binding.wifiWarning.isVisible = wifiWasVisible
    }
}