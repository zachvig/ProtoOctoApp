package de.crysxd.octoapp.signin.discover

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.DiscoverFragmentInitialBinding
import de.crysxd.octoapp.signin.di.injectViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.math.floor
import de.crysxd.octoapp.base.di.Injector as BaseInjector

class DiscoverFragment : BaseFragment() {
    override val viewModel by injectViewModel<DiscoverViewModel>()
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(BaseInjector.get().viewModelFactory())
    private lateinit var binding: DiscoverFragmentInitialBinding
    private var continueManuallyJob: Job? = null
    var viewMovedToSecondaryLayout = false

    companion object {
        const val INITIAL_LOADING_DELAY_MS = 2000L
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        DiscoverFragmentInitialBinding.inflate(inflater, container, false).also {
            binding = it
            viewMovedToSecondaryLayout = false
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scheduleContinueManually()
        binding.needHelp.setOnClickListener {
            continueWithHelp()
        }
        binding.content.manualConnectOption.showManualConnect()
        binding.content.quickSwitchOption.showQuickSwitchOption()
        binding.content.quickSwitchOption.setOnClickListener {
            continueWithEnableQuickSwitch()
        }
        binding.content.manualConnectOption.setOnClickListener { continueWithManualConnect() }
        binding.content.buttonDelete.setOnClickListener {
            beginDelayedTransition()
            binding.content.buttonDelete.isVisible = false
            binding.content.previousOptions.forEach {
                if (it != binding.content.quickSwitchOption) {
                    (it as? DiscoverOptionView)?.showDelete()
                }
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            beginDelayedTransition()

            // First and previously connected? Instantly move to secondary layout
            if (it.connectedOctoPrint.isNotEmpty()) {
                moveToSecondaryLayout()
                continueManuallyJob?.cancel()
            }


            createDiscoveredOptions(it.discoveredOctoPrint)
            createPreviouslyConnectedOptions(it.connectedOctoPrint, it.supportsQuickSwitch)
        }


        wifiViewModel.networkState.observe(viewLifecycleOwner) {
            binding.wifiWarning.isVisible = it is NetworkStateViewModel.NetworkState.WifiNotConnected
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        binding.scrollView.setupWithToolbar(requireOctoActivity())
    }

    private fun beginDelayedTransition() = TransitionManager.beginDelayedTransition(binding.root, InstantAutoTransition())

    private fun scheduleContinueManually() {
        // Delay the manual button a bit so we hopefully automatically find OctoPrint
        continueManuallyJob = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val delay = INITIAL_LOADING_DELAY_MS
            val steps = 200
            val stepDelay = floor(delay / steps.toFloat()).toLong()
            binding.progressBar.max = steps
            repeat(steps) {
                delay(stepDelay)
                binding.progressBar.progress = it + 1
            }
            binding.progressBar.isIndeterminate = true

            if (binding.content.discoveredOptions.childCount > 0 || binding.content.previousOptions.childCount > 1) {
                beginDelayedTransition()
                moveToSecondaryLayout()
            } else {
                continueWithManualConnect()
            }
        }
    }

    private fun createDiscoveredOptions(options: List<DiscoverOctoPrintUseCase.DiscoveredOctoPrint>) {
        // Delete all that are not longer shown
        binding.content.discoveredOptions.children.toList().forEach {
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
    }

    private fun createPreviouslyConnectedOptions(options: List<OctoPrintInstanceInformationV2>, quickSwitchEnabled: Boolean) {
        // Delete all that are not longer shown
        binding.content.previousOptions.children.toList().forEach {
            if (it == binding.content.quickSwitchOption) return@forEach
            else if (it !is DiscoverOptionView) binding.content.previousOptions.removeView(it)
            else if (!options.any { o -> it.isShowing(o) }) binding.content.previousOptions.removeView(it)
        }

        // Add all missing
        options.map {
            binding.content.previousOptions.children.firstOrNull { view ->
                view is DiscoverOptionView && view.isShowing(it)
            } ?: DiscoverOptionView(requireContext()).apply {
                show(it, quickSwitchEnabled)
                setOnClickListener { _ ->
                    if (quickSwitchEnabled) {
                        continueWithPreviouslyConnected(it)
                    } else {
                        continueWithEnableQuickSwitch()
                    }
                }
                onDelete = { deleteAfterConfirmation(it) }
            }
        }.filter {
            it.parent == null
        }.forEach {
            binding.content.previousOptions.addView(it, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        // Update title visibility
        binding.content.previousOptionsTitle.isVisible = binding.content.previousOptions.childCount > 1
        binding.content.quickSwitchOption.isVisible = !quickSwitchEnabled && binding.content.previousOptionsTitle.isVisible
        binding.content.buttonDelete.isVisible = binding.content.buttonDelete.isVisible && binding.content.previousOptionsTitle.isVisible
    }

    private fun continueWithDiscovered(octoPrint: DiscoverOctoPrintUseCase.DiscoveredOctoPrint) {
        Toast.makeText(requireContext(), octoPrint.label, Toast.LENGTH_SHORT).show()
    }

    private fun continueWithPreviouslyConnected(octoPrint: OctoPrintInstanceInformationV2) {
        viewModel.activatePreviouslyConnected(octoPrint)
    }

    private fun continueWithManualConnect() {
        findNavController().navigate(R.id.action_manual_sign_in)
    }

    private fun continueWithHelp() {
        val uri = Uri.parse(Firebase.remoteConfig.getString("help_url_sign_in"))
        OctoAnalytics.logEvent(OctoAnalytics.Event.SignInHelpOpened)
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun continueWithEnableQuickSwitch() = UriLibrary.getPurchaseUri().open(requireOctoActivity())

    private fun deleteAfterConfirmation(option: OctoPrintInstanceInformationV2) {
        requireOctoActivity().showDialog(
            message = getString(R.string.signin___discovery___delete_printer_message, option.label),
            positiveButton = getString(R.string.signin___discovery_delete_printer_confirmation),
            positiveAction = { viewModel.deleteInstance(option.webUrl) },
            neutralAction = {},
            neutralButton = getString(R.string.cancel)
        )
    }

    private fun moveToSecondaryLayout() {
        viewMovedToSecondaryLayout = true
        val wifiWasVisible = binding.wifiWarning.isVisible
        ConstraintSet().let {
            it.load(requireContext(), R.layout.discover_fragment)
            it.applyTo(binding.root)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.title.setTextAppearance(R.style.OctoTheme_TextAppearance_Title)
            binding.subtitle.setTextAppearance(R.style.OctoTheme_TextAppearance)
            binding.subtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.light_text))
        }
        binding.subtitle.text = getString(R.string.signin___discovery___welcome_subtitle_select_option)
        binding.wifiWarning.isVisible = wifiWasVisible
    }
}