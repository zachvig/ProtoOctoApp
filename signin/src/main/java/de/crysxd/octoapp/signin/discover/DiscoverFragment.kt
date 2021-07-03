package de.crysxd.octoapp.signin.discover

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionInflater
import android.transition.TransitionManager
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requestFocusAndOpenSoftKeyboard
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.signin.BuildConfig
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.BaseSigninFragmentBinding
import de.crysxd.octoapp.signin.databinding.DiscoverFragmentContentManualBinding
import de.crysxd.octoapp.signin.databinding.DiscoverFragmentContentOptionsBinding
import de.crysxd.octoapp.signin.di.injectViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber
import de.crysxd.octoapp.base.di.Injector as BaseInjector

class DiscoverFragment : BaseFragment() {
    override val viewModel by injectViewModel<DiscoverViewModel>()
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(BaseInjector.get().viewModelFactory())
    private lateinit var binding: BaseSigninFragmentBinding
    private var optionsBinding: DiscoverFragmentContentOptionsBinding? = null
    private var manualBinding: DiscoverFragmentContentManualBinding? = null
    private var loadingAnimationJob: Job? = null
    private var backgroundAlpha = 1f
    private val moveBackToOptionsBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = viewModel.moveToOptionsState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.sign_in_shard_element)
        sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(R.transition.sign_in_shard_element)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        BaseSigninFragmentBinding.inflate(inflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, moveBackToOptionsBackPressedCallback)
        backgroundAlpha = binding.octoBackground.alpha

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

                is DiscoverViewModel.UiState.Manual -> moveToManualLayout(
                    webUrl = (it as? DiscoverViewModel.UiState.ManualSuccess)?.webUrl ?: "",
                    openSoftKeyboard = it !is DiscoverViewModel.UiState.ManualSuccess
                )
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

        // Disable back button, we can't go back here
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = requireActivity().finish()
        })
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
        loadingAnimationJob = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val duration = 600L
            binding.octoBackground.alpha = 0f
            binding.loading.title.setText(R.string.signin___discovery___welcome_title)
            binding.loading.subtitle.setText(R.string.signin___discovery___welcome_subtitle_searching)
            binding.loading.title.alpha = 0f
            binding.loading.subtitle.alpha = 0f

            delay(duration)
            binding.octoBackground.animate().alpha(backgroundAlpha).setDuration(duration).withEndAction {
                binding.octoView.swim()
            }.start()
            delay(150)
            binding.loading.title.animate().alpha(1f).setDuration(duration).start()
            delay(150)
            binding.loading.subtitle.animate().alpha(1f).setDuration(duration).start()
        }

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
        binding.quickSwitchOption.setOnClickListener { continueWithPurchase() }
    }

    private fun continueWithPurchase() {
        UriLibrary.getPurchaseUri().open(requireOctoActivity())
    }

    private fun continueWithDiscovered(octoPrint: DiscoverOctoPrintUseCase.DiscoveredOctoPrint) {
        continueWithManualConnect(octoPrint.webUrl)
    }

    private fun continueWithPreviouslyConnected(octoPrint: OctoPrintInstanceInformationV2) {
        viewModel.activatePreviouslyConnected(octoPrint)
    }

    private fun continueWithManualConnect(webUrl: String, confirmed: Boolean = false) {
        if (Uri.parse(webUrl).host?.endsWith("octoeverywhere.com") == true && !confirmed) {
            requireOctoActivity().showDialog(
                message = "To get the most out of OctoApp and OctoEverywhere, connect your printer using the local web URL first. Once everything is set up, you can log in which your OctoEverywhere account.",
                neutralButton = "Connect local first",
                neutralAction = {},
                positiveAction = { continueWithManualConnect(webUrl, true) },
                positiveButton = "Connect OctoEverywhere now"
            )
            return
        }

        manualBinding?.input?.hideSoftKeyboard()
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            delay(200)

            // Start the "fix" flow, it will test the connection to the given URL
            // We do not allow the API key to be reused to prevent the user from bypassing quick switch.
            // If the user has BillingManager.FEATURE_QUICK_SWITCH, the fix flow will always allow API key reuse
            val extras = FragmentNavigatorExtras(binding.octoView to "octoView", binding.octoBackground to "octoBackground")
            val directions = DiscoverFragmentDirections.probeConnection(baseUrl = webUrl, allowApiKeyReuse = false.toString())
            findNavController().navigate(directions, extras)
        }
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
            positiveButton = getString(R.string.signin___discovery___delete_printer_confirmation),
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

        binding.content.removeAllViews()
        val localOptionsBinding = optionsBinding ?: let {
            DiscoverFragmentContentOptionsBinding.inflate(LayoutInflater.from(requireContext()), binding.content, false)
        }
        binding.content.removeAllViews()
        binding.content.addView(localOptionsBinding.root)
        binding.contentWrapper.updateLayoutParams<FrameLayout.LayoutParams> { gravity = Gravity.CENTER_VERTICAL }
        optionsBinding = localOptionsBinding

        //   manualBinding?.input?.hideSoftKeyboard()
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

    private fun moveToManualLayout(webUrl: String, openSoftKeyboard: Boolean) {
        loadingAnimationJob?.cancel()
        binding.octoBackground.clearAnimation()
        binding.octoBackground.alpha = backgroundAlpha
        beginDelayedTransition()
        moveBackToOptionsBackPressedCallback.isEnabled = true
        binding.octoView.idle()
        binding.octoView.isVisible = false

        val localManualBinding = manualBinding ?: let {
            DiscoverFragmentContentManualBinding.inflate(LayoutInflater.from(requireContext()), binding.content, false)
        }
        binding.content.removeAllViews()
        binding.content.addView(localManualBinding.root)
        binding.contentWrapper.updateLayoutParams<FrameLayout.LayoutParams> { gravity = Gravity.TOP }
        manualBinding = localManualBinding

        // If we do not have a webURL
        if (openSoftKeyboard) {
            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                delay(300)
                manualBinding?.input?.editText?.requestFocusAndOpenSoftKeyboard()
            }
        }

        manualBinding?.input?.editText?.setText(webUrl)
        manualBinding?.input?.editText?.setSelection(webUrl.length)
        manualBinding?.input?.editText?.setOnEditorActionListener { _, _, _ ->
            manualBinding?.buttonContinue?.performClick()
            true
        }

        if (BuildConfig.DEBUG) {
            manualBinding?.input?.editText?.setText("octoprint.home:5000")
        }

        localManualBinding.buttonContinue.setOnClickListener {
            viewModel.testWebUrl(localManualBinding.input.editText.text?.toString() ?: "")
        }
    }
}
