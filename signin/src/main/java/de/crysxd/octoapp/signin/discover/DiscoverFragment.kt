package de.crysxd.octoapp.signin.discover

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.*
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
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.floor
import de.crysxd.octoapp.base.di.Injector as BaseInjector

class DiscoverFragment : BaseFragment() {
    override val viewModel by injectViewModel<DiscoverViewModel>()
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(BaseInjector.get().viewModelFactory())
    private lateinit var binding: DiscoverFragmentInitialBinding
    private val moveBackToOptionsBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = viewModel.moveToOptionsState()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        DiscoverFragmentInitialBinding.inflate(inflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.needHelp.setOnClickListener {
            continueWithHelp()
        }
        binding.options.manualConnectOption.showManualConnect()
        binding.options.manualConnectOption.setOnClickListener { viewModel.moveToManualState() }
        binding.options.quickSwitchOption.showQuickSwitchOption()
        binding.options.quickSwitchOption.setOnClickListener {
            continueWithEnableQuickSwitch()
        }
        binding.options.buttonDelete.setOnClickListener {
            beginDelayedTransition()
            binding.options.buttonDelete.isVisible = false
            binding.options.previousOptions.forEach {
                if (it != binding.options.quickSwitchOption) {
                    (it as? DiscoverOptionView)?.showDelete()
                }
            }
        }
        binding.manual.buttonContinue.setOnClickListener {
            viewModel.testWebUrl(binding.manual.input.editText.text?.toString() ?: "")
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, moveBackToOptionsBackPressedCallback)

        playLoadingAnimation()
        viewModel.uiState.observe(viewLifecycleOwner) {
            beginDelayedTransition()
            when (it) {
                DiscoverViewModel.UiState.Loading -> playLoadingAnimation()
                is DiscoverViewModel.UiState.Options -> {
                    createDiscoveredOptions(it.discoveredOptions)
                    createPreviouslyConnectedOptions(it.previouslyConnectedOptions, it.supportsQuickSwitch)
                    moveToOptionsLayout()
                }
                is DiscoverViewModel.UiState.Manual -> moveToManualLayout()
            }

            if (it is DiscoverViewModel.UiState.ManualError) {
                showManualError(it)
            }

            if (it is DiscoverViewModel.UiState.ManualSuccess) {
                continueWithManualConnect(it.webUrl)
            }
        }

        wifiViewModel.networkState.observe(viewLifecycleOwner) {
            Timber.i("Wifi state: $it")
            binding.wifiWarning.isVisible = it is NetworkStateViewModel.NetworkState.WifiNotConnected
        }
    }

    private fun showManualError(it: DiscoverViewModel.UiState.ManualError) {
        it.handled = true
        if (!it.message.isNullOrBlank()) {
            requireOctoActivity().showDialog(
                message = it.message,
                neutralButton = getString(R.string.show_details),
                neutralAction = { _ ->
                    requireOctoActivity().showErrorDetailsDialog(it.exception, offerSupport = it.errorCount > 1)
                }
            )
        } else {
            requireOctoActivity().showErrorDetailsDialog(it.exception, offerSupport = it.errorCount > 1)
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        binding.scrollView.setupWithToolbar(requireOctoActivity())
        if (binding.manual.input.isVisible) {
            binding.manual.input.showSoftKeyboard()
        }
    }

    override fun onStop() {
        super.onStop()
        binding.manual.input.hideSoftKeyboard()
    }

    private fun beginDelayedTransition() = TransitionManager.beginDelayedTransition(binding.root, InstantAutoTransition(
        explode = true,
        explodeEpicenter = Rect(binding.root.width / 2, 0, binding.root.width / 2, 0)
    ).also {
        // We accidentally also animated the label changes when the keyboard was automatically opened
        it.excludeChildren(binding.manual.input, true)
    })

    private fun playLoadingAnimation() {
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val steps = 200
            val delay = DiscoverViewModel.INITIAL_DELAY_TIME
            val stepDelay = floor(delay / steps.toFloat()).toLong()
            binding.octoView.swim()
            binding.progressBar.max = steps
            binding.progressBar.isIndeterminate = false
            repeat(steps) {
                delay(stepDelay)
                binding.progressBar.progress = it + 1
            }
            binding.progressBar.isIndeterminate = true
            binding.octoView.idle()
        }
    }

    private fun createDiscoveredOptions(options: List<DiscoverOctoPrintUseCase.DiscoveredOctoPrint>) {
        // Delete all that are not longer shown
        binding.options.discoveredOptions.children.toList().forEach {
            if (it !is DiscoverOptionView) binding.options.discoveredOptions.removeView(it)
            else if (!options.any { o -> it.isShowing(o) }) binding.options.discoveredOptions.removeView(it)
        }

        // Add all missing
        options.map {
            binding.options.discoveredOptions.children.firstOrNull { view ->
                view is DiscoverOptionView && view.isShowing(it)
            } ?: DiscoverOptionView(requireContext()).apply {
                show(it)
                setOnClickListener { _ -> continueWithDiscovered(it) }
            }
        }.filter {
            it.parent == null
        }.forEach {
            binding.options.discoveredOptions.addView(it, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        // Update title visibility
        binding.options.discoveredOptionsTitle.isVisible = binding.options.discoveredOptions.childCount > 0
    }

    private fun createPreviouslyConnectedOptions(options: List<OctoPrintInstanceInformationV2>, quickSwitchEnabled: Boolean) {
        // Delete all that are not longer shown
        binding.options.previousOptions.children.toList().forEach {
            if (it == binding.options.quickSwitchOption) return@forEach
            else if (it !is DiscoverOptionView) binding.options.previousOptions.removeView(it)
            else if (!options.any { o -> it.isShowing(o) }) binding.options.previousOptions.removeView(it)
        }

        // Add all missing
        options.map {
            binding.options.previousOptions.children.firstOrNull { view ->
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
            binding.options.previousOptions.addView(it, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        // Update title visibility
        binding.options.previousOptionsTitle.isVisible = binding.options.previousOptions.childCount > 1
        binding.options.quickSwitchOption.isVisible = !quickSwitchEnabled && binding.options.previousOptionsTitle.isVisible
        binding.options.buttonDelete.isVisible = binding.options.buttonDelete.isVisible && binding.options.previousOptionsTitle.isVisible
    }

    private fun continueWithDiscovered(octoPrint: DiscoverOctoPrintUseCase.DiscoveredOctoPrint) {
        findNavController().navigate(DiscoverFragmentDirections.probeWebUrl(octoPrint.webUrl))
    }

    private fun continueWithPreviouslyConnected(octoPrint: OctoPrintInstanceInformationV2) {
        viewModel.activatePreviouslyConnected(octoPrint)
    }

    private fun continueWithManualConnect(webUrl: String) {
        findNavController().navigate(DiscoverFragmentDirections.probeWebUrl(webUrl))
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
        binding.octoView.idle()
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
        binding.wifiWarning.isVisible = wifiWasVisible
    }

    private fun moveToOptionsLayout() {
        beginDelayedTransition()
        moveToSecondaryLayout()
        binding.subtitle.text = getString(R.string.signin___discovery___welcome_subtitle_select_option)
        binding.options.root.isVisible = true
        binding.manual.root.isVisible = false
        moveBackToOptionsBackPressedCallback.isEnabled = false
    }

    private fun moveToManualLayout() {
        beginDelayedTransition()
        moveToSecondaryLayout()
        binding.subtitle.text = "Enter your OctoPrint web URL"
        binding.options.root.isVisible = false
        binding.manual.root.isVisible = true
        binding.manual.input.showSoftKeyboard()
        moveBackToOptionsBackPressedCallback.isEnabled = true
    }
}