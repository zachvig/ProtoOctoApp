package de.crysxd.octoapp.signin.discover

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.*
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
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.DiscoverFragmentBinding
import de.crysxd.octoapp.signin.databinding.DiscoverFragmentContentManualBinding
import de.crysxd.octoapp.signin.databinding.DiscoverFragmentContentOptionsBinding
import de.crysxd.octoapp.signin.di.injectViewModel
import timber.log.Timber
import de.crysxd.octoapp.base.di.Injector as BaseInjector

class DiscoverFragment : BaseFragment() {
    override val viewModel by injectViewModel<DiscoverViewModel>()
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(BaseInjector.get().viewModelFactory())
    private lateinit var binding: DiscoverFragmentBinding
    private var optionsBinding: DiscoverFragmentContentOptionsBinding? = null
    private var manualBinding: DiscoverFragmentContentManualBinding? = null
    private val moveBackToOptionsBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = viewModel.moveToOptionsState()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        DiscoverFragmentBinding.inflate(inflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, moveBackToOptionsBackPressedCallback)

        playLoadingAnimation()
        optionsBinding = null
        manualBinding = null
        viewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                DiscoverViewModel.UiState.Loading -> playLoadingAnimation()

                is DiscoverViewModel.UiState.Options -> {
                    moveToOptionsLayout()
                    createDiscoveredOptions(it.discoveredOptions)
                    createPreviouslyConnectedOptions(it.previouslyConnectedOptions, it.supportsQuickSwitch)
                }

                is DiscoverViewModel.UiState.Manual -> moveToManualLayout((it as? DiscoverViewModel.UiState.ManualSuccess)?.webUrl ?: "")
            }

            if (it is DiscoverViewModel.UiState.ManualError && !it.handled) {
                it.handled = true
                showManualError(it)
            }

            if (it is DiscoverViewModel.UiState.ManualSuccess && !it.handled) {
                it.handled = true
                continueWithManualConnect(it.webUrl)
            }
        }

        wifiViewModel.networkState.observe(viewLifecycleOwner) {
            Timber.i("Wifi state: $it")
            binding.wifiWarning.isVisible = it is NetworkStateViewModel.NetworkState.WifiNotConnected
        }
    }

    private fun showManualError(it: DiscoverViewModel.UiState.ManualError) {
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
        if (viewModel.uiState.value is DiscoverViewModel.UiState.Manual) {
            manualBinding?.input?.showSoftKeyboard()
        }
    }

    override fun onStop() {
        super.onStop()
        manualBinding?.input?.hideSoftKeyboard()
    }

    private fun beginDelayedTransition() = TransitionManager.beginDelayedTransition(binding.root, AutoTransition().also {
        it.interpolator = DecelerateInterpolator()
        manualBinding?.input?.let { v -> it.excludeChildren(v, true) }
    })

    private fun playLoadingAnimation() {
        val duration = 600L
        binding.octoBackground.alpha = 0f
        binding.loading.title.alpha = 0f
        binding.loading.subtitle.alpha = 0f
        //   binding.octoView.swim()

        binding.octoBackground.animate().alpha(1f).setDuration(duration).setStartDelay(duration).start()
        binding.loading.title.animate().alpha(1f).setDuration(duration).setStartDelay(duration + 150).start()
        binding.loading.subtitle.animate().alpha(1f).setDuration(duration).setStartDelay(duration + 300).start()
    }

    private fun createDiscoveredOptions(options: List<DiscoverOctoPrintUseCase.DiscoveredOctoPrint>) = optionsBinding?.let { binding ->
        // Delete all that are not longer shown
        binding.discoveredOptions.children.toList().forEach {
            if (it !is DiscoverOptionView) binding.discoveredOptions.removeView(it)
            else if (!options.any { o -> it.isShowing(o) }) binding.discoveredOptions.removeView(it)
        }

        // Add all missing
        options.map {
            binding.discoveredOptions.children.firstOrNull { view ->
                view is DiscoverOptionView && view.isShowing(it)
            } ?: DiscoverOptionView(requireContext()).apply {
                show(it)
                setOnClickListener { _ -> continueWithDiscovered(it) }
            }
        }.filter {
            it.parent == null
        }.forEach {
            binding.discoveredOptions.addView(it, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        // Update title visibility
        binding.discoveredOptionsTitle.isVisible = binding.discoveredOptions.childCount > 0
    }

    private fun createPreviouslyConnectedOptions(options: List<OctoPrintInstanceInformationV2>, quickSwitchEnabled: Boolean) = optionsBinding?.let { binding ->
        // Delete all that are not longer shown
        binding.previousOptions.children.toList().forEach {
            if (it == binding.quickSwitchOption) return@forEach
            else if (it !is DiscoverOptionView) binding.previousOptions.removeView(it)
            else if (!options.any { o -> it.isShowing(o) }) binding.previousOptions.removeView(it)
        }

        // Add all missing
        options.map {
            binding.previousOptions.children.firstOrNull { view ->
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
            binding.previousOptions.addView(it, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        // Update title visibility
        binding.previousOptionsTitle.isVisible = binding.previousOptions.childCount > 1
        binding.quickSwitchOption.isVisible = !quickSwitchEnabled && binding.previousOptionsTitle.isVisible
        binding.buttonDelete.isVisible = binding.buttonDelete.isVisible && binding.previousOptionsTitle.isVisible
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

    private fun moveToOptionsLayout() {
        beginDelayedTransition()
        binding.octoView.idle()
        moveBackToOptionsBackPressedCallback.isEnabled = false
        binding.octoView.isVisible = true
        binding.octoBackground.alpha = 0.5f

        binding.content.removeAllViews()
        val localOptionsBinding = optionsBinding ?: let {
            DiscoverFragmentContentOptionsBinding.inflate(LayoutInflater.from(requireContext()), binding.content, false)
        }
        binding.content.removeAllViews()
        binding.content.addView(localOptionsBinding.root)
        binding.contentWrapper.updateLayoutParams<FrameLayout.LayoutParams> { gravity = Gravity.CENTER_VERTICAL }
        optionsBinding = localOptionsBinding

        manualBinding?.input?.hideSoftKeyboard()
        localOptionsBinding.help.setOnClickListener {
            continueWithHelp()
        }
        localOptionsBinding.manualConnectOption.showManualConnect()
        localOptionsBinding.manualConnectOption.setOnClickListener { viewModel.moveToManualState() }
        localOptionsBinding.quickSwitchOption.showQuickSwitchOption()
        localOptionsBinding.quickSwitchOption.setOnClickListener {
            continueWithEnableQuickSwitch()
        }
        localOptionsBinding.buttonDelete.setOnClickListener {
            beginDelayedTransition()
            localOptionsBinding.buttonDelete.isVisible = false
            localOptionsBinding.previousOptions.forEach {
                if (it != localOptionsBinding.quickSwitchOption) {
                    (it as? DiscoverOptionView)?.showDelete()
                }
            }
        }
    }

    private fun moveToManualLayout(webUrl: String) {
        beginDelayedTransition()
        moveBackToOptionsBackPressedCallback.isEnabled = true
        binding.octoView.idle()
        binding.octoView.isVisible = false
        binding.octoBackground.alpha = 0.25f

        val localManualBinding = manualBinding ?: let {
            DiscoverFragmentContentManualBinding.inflate(LayoutInflater.from(requireContext()), binding.content, false)
        }
        binding.content.removeAllViews()
        binding.content.addView(localManualBinding.root)
        binding.contentWrapper.updateLayoutParams<FrameLayout.LayoutParams> { gravity = Gravity.TOP }
        manualBinding = localManualBinding
        manualBinding?.input?.showSoftKeyboard()
        manualBinding?.input?.editText?.setText(webUrl)
        manualBinding?.input?.editText?.setSelection(webUrl.length)


        localManualBinding.buttonContinue.setOnClickListener {
            viewModel.testWebUrl(localManualBinding.input.editText.text?.toString() ?: "")
        }
    }
}
